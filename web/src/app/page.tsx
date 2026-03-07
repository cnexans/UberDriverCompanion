import { OverlayDemo } from "@/components/overlay-demo";
import { MetricCard } from "@/components/metric-card";
import { StepCard } from "@/components/step-card";
import { FeatureCard } from "@/components/feature-card";
import { ArchDiagram } from "@/components/arch-diagram";

export default function Home() {
  return (
    <div className="min-h-screen">
      {/* Hero */}
      <header className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-green-950/40 via-transparent to-emerald-950/30" />
        <nav className="relative z-10 max-w-6xl mx-auto px-6 py-6 flex items-center justify-between">
          <span className="text-lg font-bold tracking-tight">
            <span className="text-green-400">Uber</span>DriverCompanion
          </span>
          <a
            href="https://github.com/cnexans/UberDriverCompanion"
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-neutral-400 hover:text-white transition-colors flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
            </svg>
            GitHub
          </a>
        </nav>

        <div className="relative z-10 max-w-6xl mx-auto px-6 pt-20 pb-32">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-green-500/10 border border-green-500/20 text-green-400 text-sm mb-6">
                <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                Open source
              </div>
              <h1 className="text-5xl lg:text-6xl font-bold tracking-tight leading-[1.1] mb-6">
                Sabe cuanto{" "}
                <span className="text-green-400">ganas por km</span> antes de
                aceptar
              </h1>
              <p className="text-lg text-neutral-400 leading-relaxed mb-8 max-w-lg">
                Un overlay flotante que analiza cada viaje de Uber en tiempo
                real. Ve $/km, $/hora y que porcentaje del recorrido es pago,
                directo en tu pantalla.
              </p>
              <div className="flex flex-wrap gap-4">
                <a
                  href="https://github.com/cnexans/UberDriverCompanion/releases"
                  className="inline-flex items-center gap-2 px-6 py-3 bg-green-500 hover:bg-green-400 text-black font-semibold rounded-lg transition-colors"
                >
                  <svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
                    />
                  </svg>
                  Descargar APK
                </a>
                <a
                  href="https://github.com/cnexans/UberDriverCompanion"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-6 py-3 border border-neutral-700 hover:border-neutral-500 text-neutral-300 hover:text-white font-medium rounded-lg transition-colors"
                >
                  Ver codigo fuente
                </a>
              </div>
            </div>

            <div className="flex justify-center lg:justify-end">
              <OverlayDemo />
            </div>
          </div>
        </div>
      </header>

      {/* Metrics section */}
      <section className="py-24 border-t border-neutral-800">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-center mb-4">
            Tres metricas, una decision
          </h2>
          <p className="text-neutral-400 text-center max-w-2xl mx-auto mb-16">
            Cada viaje se analiza al instante. El overlay te muestra lo que
            necesitas saber con colores claros: verde es bueno, rojo es malo.
          </p>

          <div className="grid md:grid-cols-3 gap-6">
            <MetricCard
              label="$/km"
              value="378"
              description="Cuanto ganas por cada kilometro recorrido (incluyendo el viaje hasta el pasajero)."
              good="≥ $300"
              bad="< $300"
              color="green"
            />
            <MetricCard
              label="$/hora"
              value="8.690"
              description="Proyeccion de ganancia por hora basada en el tiempo total del viaje."
              good="≥ $6.000"
              bad="< $6.000"
              color="green"
            />
            <MetricCard
              label="% dist. cobrada"
              value="54%"
              description="Que porcentaje del recorrido total es pagado. Si recorres 10km pero solo te pagan 3km, es 30%."
              good="≥ 60%"
              bad="< 40%"
              color="yellow"
            />
          </div>
        </div>
      </section>

      {/* Problem / Solution */}
      <section className="py-24 border-t border-neutral-800 bg-neutral-950/50">
        <div className="max-w-6xl mx-auto px-6">
          <div className="grid lg:grid-cols-2 gap-16">
            <div>
              <h2 className="text-3xl font-bold mb-6">El problema</h2>
              <div className="space-y-4 text-neutral-400 leading-relaxed">
                <p>
                  Uber te muestra un viaje con un precio y un mapa. Tenes{" "}
                  <strong className="text-white">15 segundos</strong> para
                  decidir.
                </p>
                <p>
                  Pero $2.000 puede ser excelente o pesimo dependiendo de cuanto
                  tengas que manejar. Un viaje de $2.000 a 3km es genial. El
                  mismo precio con 8km de recogida + 5km de viaje es terrible.
                </p>
                <p>
                  Hacer esa cuenta mental en 15 segundos, mientras manejas, es
                  imposible.
                </p>
              </div>
            </div>
            <div>
              <h2 className="text-3xl font-bold mb-6 text-green-400">
                La solucion
              </h2>
              <div className="space-y-4 text-neutral-400 leading-relaxed">
                <p>
                  UberDriverCompanion lee la pantalla automaticamente y hace los
                  calculos por vos. En menos de{" "}
                  <strong className="text-white">1 segundo</strong> tenes las
                  metricas en pantalla.
                </p>
                <p>
                  No toca la app de Uber, no modifica nada, no necesita root. Es
                  un lector de pantalla que usa OCR para entender lo que dice el
                  popup del viaje.
                </p>
                <p>
                  Funciona con{" "}
                  <strong className="text-white">
                    Moto, Auto, Exclusivo y Articulo
                  </strong>
                  . Detecta bonus, identidad verificada y rating del pasajero.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-24 border-t border-neutral-800">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-center mb-16">
            Caracteristicas
          </h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            <FeatureCard
              icon="⚡"
              title="Tiempo real"
              description="Analiza cada viaje en menos de 1 segundo. Screenshots cada 100ms durante el popup."
            />
            <FeatureCard
              icon="📊"
              title="Historial"
              description="Guarda cada viaje ofrecido en base de datos local. Ve estadisticas del dia desde la app."
            />
            <FeatureCard
              icon="🔒"
              title="100% local"
              description="Todo corre en tu telefono. No envia datos a ningun servidor. OCR on-device via ML Kit."
            />
            <FeatureCard
              icon="🎯"
              title="Overlay flotante"
              description="Se muestra sobre Uber sin interferir. Arrastralo a donde quieras, minimizalo con un tap."
            />
            <FeatureCard
              icon="🏍️"
              title="Todos los tipos"
              description="Funciona con Moto, Auto, Exclusivo, Articulo y Flash. Detecta bonus e identidad."
            />
            <FeatureCard
              icon="🔓"
              title="Open source"
              description="Codigo 100% abierto. Sin publicidad, sin tracking, sin cuentas. Descarga y usa."
            />
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="py-24 border-t border-neutral-800 bg-neutral-950/50">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-center mb-4">
            Como funciona
          </h2>
          <p className="text-neutral-400 text-center max-w-2xl mx-auto mb-16">
            Usa el servicio de accesibilidad de Android para detectar cuando Uber
            muestra un viaje, y OCR para leer los datos de la pantalla.
          </p>

          <div className="grid md:grid-cols-4 gap-6 mb-16">
            <StepCard
              step={1}
              title="Detecta el popup"
              description="El servicio de accesibilidad monitorea Uber Driver y detecta cuando aparece un viaje nuevo."
            />
            <StepCard
              step={2}
              title="Captura pantalla"
              description="Toma un screenshot instantaneo usando la API de Android (no necesita root)."
            />
            <StepCard
              step={3}
              title="Lee con OCR"
              description="ML Kit analiza la imagen y extrae precio, distancias, tiempos, tipo y rating."
            />
            <StepCard
              step={4}
              title="Muestra metricas"
              description="Calcula $/km, $/hora y % distancia cobrada, y actualiza el overlay en pantalla."
            />
          </div>

          <ArchDiagram />
        </div>
      </section>

      {/* Setup */}
      <section className="py-24 border-t border-neutral-800">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-center mb-4">
            Setup en 3 pasos
          </h2>
          <p className="text-neutral-400 text-center max-w-2xl mx-auto mb-16">
            No necesitas root, no necesitas PC, no necesitas cuenta.
          </p>

          <div className="max-w-2xl mx-auto space-y-6">
            <div className="flex gap-4 items-start p-6 rounded-xl bg-neutral-900/50 border border-neutral-800">
              <span className="flex-shrink-0 w-10 h-10 rounded-full bg-green-500/10 border border-green-500/30 text-green-400 flex items-center justify-center font-bold">
                1
              </span>
              <div>
                <h3 className="font-semibold mb-1">Instala el APK</h3>
                <p className="text-neutral-400 text-sm">
                  Descarga desde{" "}
                  <a
                    href="https://github.com/cnexans/UberDriverCompanion/releases"
                    className="text-green-400 hover:underline"
                  >
                    GitHub Releases
                  </a>{" "}
                  o compila desde el codigo fuente.
                </p>
              </div>
            </div>
            <div className="flex gap-4 items-start p-6 rounded-xl bg-neutral-900/50 border border-neutral-800">
              <span className="flex-shrink-0 w-10 h-10 rounded-full bg-green-500/10 border border-green-500/30 text-green-400 flex items-center justify-center font-bold">
                2
              </span>
              <div>
                <h3 className="font-semibold mb-1">Habilita los permisos</h3>
                <p className="text-neutral-400 text-sm">
                  Activa el servicio de accesibilidad y el permiso de overlay
                  desde la app. Dos botones, dos taps.
                </p>
              </div>
            </div>
            <div className="flex gap-4 items-start p-6 rounded-xl bg-neutral-900/50 border border-neutral-800">
              <span className="flex-shrink-0 w-10 h-10 rounded-full bg-green-500/10 border border-green-500/30 text-green-400 flex items-center justify-center font-bold">
                3
              </span>
              <div>
                <h3 className="font-semibold mb-1">Abri Uber y maneja</h3>
                <p className="text-neutral-400 text-sm">
                  El overlay aparece automaticamente cada vez que llega un viaje.
                  No tenes que hacer nada mas.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-24 border-t border-neutral-800 bg-gradient-to-b from-transparent to-green-950/20">
        <div className="max-w-3xl mx-auto px-6 text-center">
          <h2 className="text-4xl font-bold mb-4">
            Deja de adivinar, empieza a calcular
          </h2>
          <p className="text-neutral-400 text-lg mb-8">
            Cada viaje que rechazas bien es plata que no perdes. Cada viaje que
            aceptas bien es plata que ganas mejor.
          </p>
          <div className="flex flex-wrap gap-4 justify-center">
            <a
              href="https://github.com/cnexans/UberDriverCompanion/releases"
              className="inline-flex items-center gap-2 px-8 py-4 bg-green-500 hover:bg-green-400 text-black font-semibold rounded-lg transition-colors text-lg"
            >
              Descargar gratis
            </a>
            <a
              href="https://github.com/cnexans/UberDriverCompanion"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-8 py-4 border border-neutral-700 hover:border-neutral-500 text-neutral-300 hover:text-white font-medium rounded-lg transition-colors text-lg"
            >
              Ver en GitHub
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-8 border-t border-neutral-800">
        <div className="max-w-6xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-neutral-500">
          <span>
            UberDriverCompanion — Proyecto open source. No afiliado con Uber.
          </span>
          <a
            href="https://github.com/cnexans/UberDriverCompanion"
            className="hover:text-neutral-300 transition-colors"
          >
            GitHub
          </a>
        </div>
      </footer>
    </div>
  );
}
