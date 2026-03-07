# UberDriverCompanion

App Android que analiza en tiempo real los viajes ofrecidos en Uber Driver, mostrando metricas clave en un overlay flotante para ayudar al conductor a tomar mejores decisiones.

## Que hace

Cuando Uber Driver muestra un viaje disponible, la app:

1. **Captura la pantalla** via AccessibilityService + screenshot + OCR (ML Kit)
2. **Extrae los datos** del viaje: precio, distancia, tiempo, tipo, rating del pasajero
3. **Calcula metricas** y las muestra en un overlay flotante sobre Uber:
   - **$/km** — Pesos por kilometro total (recogida + viaje)
   - **$/h** — Pesos por hora estimados
   - **% distancia cobrada** — Que porcentaje del recorrido total es pagado por el pasajero
4. **Guarda historial** de todos los viajes ofrecidos en base de datos local

## Screenshots

El overlay flotante se muestra sobre Uber Driver con las metricas en tiempo real:

```
  ▬ UBER ▬
  Moto
  $ 1.738
  $/km: 378        (verde = bueno)
  $/h: 8.690       (verde = bueno)
  Dist cobrada: 41% (rojo = mucho recorrido sin cobrar)
  Recogida: 7min (2.7km)
  Viaje: 5min (1.9km)
```

Las metricas tienen colores:
- **$/km**: Verde >= $300, Rojo < $300
- **$/h**: Verde >= $6.000, Rojo < $6.000
- **% dist cobrada**: Verde >= 60%, Amarillo >= 40%, Rojo < 40%

## Como funciona internamente

### Deteccion de viajes

Uber Driver renderiza el popup de viaje usando SurfaceView/Canvas, lo cual hace que el arbol de accesibilidad este vacio. Para leer el contenido:

1. El `AccessibilityService` monitorea eventos de `com.ubercab.driver`
2. Detecta el popup cuando el conteo de textos del root cae abruptamente (de ~15-20 a 0-5)
3. Toma un screenshot via `takeScreenshot()` (API 30+)
4. Procesa la imagen con ML Kit OCR (on-device, no requiere internet)
5. Parsea los textos extraidos con regex para obtener precio, distancias, tiempos, etc.

### Arquitectura

```
app/src/main/java/com/carlos/uberanalyzer/
├── MainActivity.kt                  # Pantalla principal, permisos, historial del dia
├── service/
│   ├── UberAccessibilityService.kt  # Lee la pantalla de Uber via screenshots + OCR
│   └── OverlayService.kt           # Burbuja flotante draggable con metricas
├── parser/
│   └── TripParser.kt               # Parsea textos OCR a TripData usando regex
├── model/
│   └── TripData.kt                 # Data class con campos y metricas calculadas
├── db/
│   ├── TripDatabase.kt             # Room database
│   ├── TripDao.kt                  # Queries (historial, stats del dia)
│   └── TripEntity.kt              # Entidad persistida
└── ui/
    └── HistoryAdapter.kt           # RecyclerView adapter para historial
```

### Datos que extrae

| Campo | Ejemplo | Regex |
|-------|---------|-------|
| Precio | `$ 2.343` | `^\$\s?([0-9.,]+)$` |
| Bonus | `+$ 464,00 incluido` | `\+\$ ([0-9.,]+) incluido` |
| Recogida | `A 7 min (2.5 km)` | `A\s?(\d+) min \(([0-9.]+) km\)` |
| Viaje | `Viaje: 8 min (2.9 km)` | `Viaje: (\d+) min \(([0-9.]+) km\)` |
| Tipo | `Moto`, `Auto`, `Exclusivo` | Busqueda por lista |
| Rating | `4,93 (779)` | `([0-9],[0-9]{2}) \((\d+)\)` |
| Identidad | `DNI verificado` | `(?i)(DNI\|Identidad\|verificad\|...)` |

### Metricas calculadas

- **$/km** = `precio / (km_recogida + km_viaje)`
- **$/h** = `(precio / (min_recogida + min_viaje)) * 60`
- **% distancia cobrada** = `km_viaje / (km_viaje + km_recogida) * 100`

## Requisitos

- Android 11+ (API 30) — necesario para `takeScreenshot()`
- Uber Driver instalado (`com.ubercab.driver`)

## Instalacion

### Desde codigo fuente

```bash
# Clonar
git clone https://github.com/cnexans/UberDriverCompanion.git
cd UberDriverCompanion

# Compilar (requiere JDK 17)
./gradlew assembleDebug

# Instalar
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Configuracion en el dispositivo

1. Abrir **UberDriverCompanion**
2. Tocar **"Habilitar Accesibilidad"** → activar "Uber Analyzer" en la lista
3. Tocar **"Permiso Overlay"** → permitir mostrar sobre otras apps
4. Abrir **Uber Driver** — el overlay aparecera automaticamente cuando llegue un viaje

## Uso

- **Drag**: Arrastra el overlay a cualquier posicion de la pantalla
- **Tap**: Toca el overlay para minimizar/expandir
- El overlay se actualiza automaticamente cada vez que aparece un viaje nuevo
- La pantalla principal muestra el historial de viajes del dia con estadisticas

## Overlay sobre Uber

El overlay no interfiere con la app de Uber. Podes:
- Aceptar/rechazar viajes normalmente
- Mover el overlay si tapa algun boton
- Minimizarlo cuando no lo necesites

## Stack

- Kotlin
- AccessibilityService + `takeScreenshot()` API
- ML Kit Text Recognition (OCR on-device)
- Room (base de datos local)
- Coroutines

## Limitaciones

- Solo funciona con Uber Driver en español (Argentina). Los regex estan adaptados al formato local (`$ X.XXX`, `A X min`, etc.)
- Requiere Android 11+ por la API de screenshots
- El OCR puede tener errores menores en textos pequeños o con fondos complejos
- Los umbrales de colores ($/km >= 300, $/h >= 6000) estan hardcodeados para el mercado argentino

## Licencia

MIT
