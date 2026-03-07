# RFC 001: Analisis de latencia en la deteccion de viajes

## Contexto

El popup de viaje en Uber Driver dura ~15 segundos. El conductor necesita ver las metricas lo mas rapido posible para tomar una decision. Este documento analiza cada etapa del pipeline, identifica cuellos de botella y propone optimizaciones.

## Pipeline actual

```
Evento accesibilidad → scanAndProcess → deteccion popup → screenshot → OCR → parse → overlay
```

### Tiempos medidos (logs reales)

| Etapa | Tiempo | Notas |
|-------|--------|-------|
| Evento → scanAndProcess | 0-250ms | Debounce de 250ms limita frecuencia |
| Deteccion de popup | <1ms | Comparacion de `rootTextCount` vs `lastNormalTextCount` |
| Screenshot API | 50-300ms | Variable, a veces falla (errorCode=1 o 3) |
| ML Kit OCR | 200-400ms | On-device, depende del contenido de la imagen |
| TripParser.parse | <1ms | Regex sobre lista de strings |
| Overlay update | <1ms | `setText()` en views existentes |
| **Total tipico** | **500-950ms** | Desde evento hasta overlay visible |
| DB write (async) | 5-20ms | No bloquea — corre en Dispatchers.IO |

### Diagrama de tiempo

```
t=0ms    Uber muestra popup de viaje
t=0ms    AccessibilityService recibe TYPE_WINDOW_CONTENT_CHANGED
t=0ms    rootTextCount cae de ~18 a ~2 (popup oculta contenido)
t=250ms  Debounce expira, scanAndProcess() corre
t=250ms  isTripPopup=true detectado
t=250ms  takeScreenshotForOCR() llamado
t=350ms  Screenshot capturado (callback onSuccess)
t=350ms  Bitmap convertido a software buffer, enviado a ML Kit
t=650ms  OCR completo, textos extraidos
t=651ms  TripParser.parse() retorna TripData
t=651ms  OverlayService.updateTrip() — metricas visibles
t=655ms  DB insert lanzado en background (no bloquea)
```

## Cuellos de botella identificados

### 1. Debounce de 250ms (DEBOUNCE_MS)

**Impacto:** Agrega hasta 250ms de latencia al primer evento.

**Analisis:** El debounce previene que `scanAndProcess` corra en cada evento de accesibilidad (pueden llegar decenas por segundo). Sin embargo, para la deteccion del popup, el primer evento es el que importa.

**Propuesta:** Reducir a 100ms. El `scanAndProcess` es liviano cuando no hay popup — la carga pesada (screenshot/OCR) ya tiene su propio cooldown de 100ms.

**Riesgo:** Mas llamadas a `scanAndProcess` cuando Uber hace scroll o animaciones. Impacto en bateria minimo porque el metodo retorna rapido si no hay trip data.

### 2. Screenshot API (50-300ms, con fallos)

**Impacto:** Principal fuente de variabilidad. A veces falla con errorCode=1 (INTERNAL_ERROR) o errorCode=3 (INTERVAL_TIME_SHORT).

**Analisis:**
- `errorCode=3` ocurre cuando se piden screenshots muy rapido (< ~300ms entre capturas). El cooldown de 100ms es mas agresivo que lo que la API permite.
- `errorCode=1` es intermitente y no tiene workaround documentado.
- Multiples screenshots se disparan en paralelo porque el callback es asincrono y no hay mutex.

**Propuesta:**
- Agregar un flag `isScreenshotInProgress` para evitar capturas concurrentes.
- En `onFailure`, reintentar una vez despues de 200ms en lugar de descartar.
- El cooldown de 100ms esta bien — los fallos de errorCode=3 se absorben con el flag de concurrencia.

**Codigo sugerido:**
```kotlin
private var isScreenshotInProgress = false

// En el bloque de popup detection:
if (!isScreenshotInProgress && now - lastScreenshotTime > SCREENSHOT_COOLDOWN_MS) {
    isScreenshotInProgress = true
    lastScreenshotTime = now
    takeScreenshotForOCR(texts.toList())
}

// En onSuccess/onFailure:
isScreenshotInProgress = false
```

### 3. ML Kit OCR (200-400ms)

**Impacto:** Es el paso mas lento pero no optimizable directamente — es procesamiento de imagen on-device.

**Analisis:**
- El bitmap es full-screen (1080x2400). El popup del viaje ocupa ~40% de la pantalla.
- ML Kit procesa toda la imagen incluyendo el mapa, barra de estado, etc.
- Los textos del mapa (nombres de calles) se mezclan con los del viaje y se filtran despues.

**Propuesta:** Recortar el bitmap antes de enviar a OCR. El popup de viaje siempre aparece en la mitad inferior de la pantalla.

```kotlin
// Crop al 50% inferior de la pantalla
val cropped = Bitmap.createBitmap(swBitmap, 0, swBitmap.height / 3,
    swBitmap.width, swBitmap.height * 2 / 3)
```

**Beneficio estimado:** 30-50% menos tiempo de OCR (menos pixeles, menos bloques de texto). Tambien reduce falsos positivos de nombres de calles.

**Riesgo:** Si Uber cambia la posicion del popup, el crop puede cortar datos. Mitigacion: usar un crop conservador (tercio superior descartado, no mitad).

### 4. Multiples OCR del mismo viaje

**Impacto:** Actualmente se toman 3-6 screenshots del mismo popup, cada uno pasa por OCR completo. Despues del primero, los siguientes no aportan informacion nueva.

**Analisis de logs:**
```
23:41:07.044 OCR_TRIP: Moto $2343.0 $/km=433.89  ← primer resultado
23:41:07.540 OCR_TRIP: Moto $2343.0 $/km=433.89  ← duplicado
23:41:08.523 OCR_TRIP: Moto $2343.0 $/km=433.89  ← duplicado
23:41:09.373 OCR_TRIP: Moto $2343.0 $/km=433.89  ← duplicado
```

**Propuesta:** Despues de detectar un trip exitosamente via OCR, pausar screenshots por 10 segundos (duracion del popup). El `lastTripKey` check ya previene actualizaciones duplicadas del overlay, pero los screenshots y OCR siguen corriendo innecesariamente.

```kotlin
private var lastOcrTripTime = 0L

// Despues de OCR exitoso:
lastOcrTripTime = System.currentTimeMillis()

// En deteccion de popup, agregar condicion:
val ocrCooldownOk = now - lastOcrTripTime > 10_000
if (ocrCooldownOk && now - lastScreenshotTime > SCREENSHOT_COOLDOWN_MS) { ... }
```

**Beneficio:** Reduce consumo de CPU/bateria significativamente durante popups largos. De 3-6 ciclos de screenshot+OCR a solo 1.

### 5. Accessibility tree traversal redundante

**Impacto:** Menor (< 5ms total), pero se hace trabajo innecesario.

**Analisis:** `scanAndProcess` hace 3 pasadas por el arbol:
1. `rootInActiveWindow` → `extractTexts`
2. `windows` loop → `extractTexts` por cada ventana
3. `findAccessibilityNodeInfosByText` con 20+ search terms

Las pasadas 1 y 2 casi nunca encuentran datos del viaje (el popup es un SurfaceView vacio). La pasada 3 a veces encuentra textos parciales pero el OCR los tiene completos.

**Propuesta:** No hacer la pasada 3 (`findByText`) si ya se detecto popup y se disparo screenshot. El OCR va a tener mejor data.

### 6. DB write (no bloqueante, sin impacto en latencia)

**Impacto en velocidad de reaccion:** Cero. Ya corre en `Dispatchers.IO`.

**Analisis:** Room insert de un TripEntity toma 5-20ms en hilo background. No compite con el hilo principal donde corre el overlay update. La base ocupa ~200 bytes por viaje, incluso con miles de registros es negligible.

**Conclusion:** No tocar. Es necesario para el futuro reporting a API.

## Resumen de optimizaciones propuestas

| # | Optimizacion | Ahorro estimado | Complejidad | Prioridad |
|---|-------------|----------------|-------------|-----------|
| 1 | Reducir DEBOUNCE_MS de 250ms a 100ms | ~150ms en primer evento | Trivial (cambiar constante) | Alta |
| 2 | Flag `isScreenshotInProgress` | Elimina screenshots concurrentes fallidos | Baja | Alta |
| 3 | Crop bitmap antes de OCR | 30-50% menos tiempo OCR (~100ms) | Baja | Media |
| 4 | Pausar screenshots 10s post-deteccion | Ahorra CPU/bateria, no latencia | Baja | Media |
| 5 | Skip findByText cuando hay popup | ~5ms + menos allocations | Baja | Baja |

### Latencia estimada post-optimizaciones

```
t=0ms    Popup aparece
t=100ms  Debounce expira (antes: 250ms)
t=100ms  Screenshot disparado
t=200ms  Screenshot capturado
t=200ms  Bitmap cropped y enviado a ML Kit
t=450ms  OCR completo (antes: 650ms con imagen completa)
t=451ms  Overlay actualizado
```

**Total estimado: ~450ms** (vs ~650ms actual). Mejora de ~30%.

## Decision

Pendiente de implementacion. Las optimizaciones 1 y 2 se pueden hacer de inmediato con riesgo minimo. La 3 (crop) requiere testing para validar que el crop no corte datos del popup en diferentes dispositivos.
