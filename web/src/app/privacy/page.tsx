import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Politica de Privacidad — Pana Uber",
};

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-200">
      <div className="max-w-3xl mx-auto px-6 py-16">
        <a
          href="/"
          className="text-green-400 hover:text-green-300 text-sm mb-8 inline-block"
        >
          &larr; Volver al inicio
        </a>

        <h1 className="text-3xl font-bold text-white mb-2">
          Politica de Privacidad
        </h1>
        <p className="text-neutral-500 mb-8">
          Ultima actualizacion: 9 de marzo de 2026
        </p>

        <div className="space-y-6 text-neutral-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              1. Informacion que recopilamos
            </h2>
            <p>
              Pana Uber (en adelante &quot;la App&quot;) no recopila, almacena ni
              transmite datos personales de los usuarios. Todo el procesamiento
              de datos de viajes (precios, distancias, tiempos) se realiza de
              forma local en el dispositivo del usuario y no se envia a ningun
              servidor externo.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              2. Analitica y telemetria
            </h2>
            <p>
              Utilizamos PostHog para recopilar datos anonimos de uso con el
              unico proposito de mejorar el servicio. Esta informacion incluye:
            </p>
            <ul className="list-disc list-inside mt-2 space-y-1 text-neutral-400">
              <li>Paginas visitadas en el sitio web</li>
              <li>Eventos de interaccion generales (clics, navegacion)</li>
              <li>Informacion tecnica del dispositivo (tipo de navegador, sistema operativo)</li>
            </ul>
            <p className="mt-2">
              Estos datos son anonimos, no identifican personalmente al usuario y
              se utilizan exclusivamente para entender como se usa la App y
              mejorar la experiencia.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              3. Datos del dispositivo
            </h2>
            <p>
              La App utiliza el Servicio de Accesibilidad de Android para leer la
              pantalla de la aplicacion Uber Driver. Esta informacion se procesa
              en tiempo real y se almacena unicamente en la base de datos local
              del dispositivo. Ningun dato de la pantalla se envia a servidores
              externos.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              4. Almacenamiento local
            </h2>
            <p>
              El historial de viajes se guarda en una base de datos SQLite local
              en el dispositivo. El usuario puede eliminar estos datos en
              cualquier momento desinstalando la aplicacion o borrando los datos
              de la app desde la configuracion de Android.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              5. Comparticion de datos con terceros
            </h2>
            <p>
              No compartimos, vendemos ni transferimos datos personales a
              terceros. La unica herramienta de terceros utilizada es PostHog
              para analitica anonima del sitio web.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              6. Seguridad
            </h2>
            <p>
              Dado que no recopilamos datos personales, el riesgo de filtracion
              es minimo. Los datos de viajes almacenados localmente estan
              protegidos por los mecanismos de seguridad del sistema operativo
              Android.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              7. Cambios a esta politica
            </h2>
            <p>
              Nos reservamos el derecho de actualizar esta politica de privacidad
              en cualquier momento. Cualquier cambio sera publicado en esta
              pagina con la fecha de actualizacion correspondiente.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              8. Contacto
            </h2>
            <p>
              Si tenes preguntas sobre esta politica de privacidad, podes
              contactarnos a traves de nuestro repositorio en{" "}
              <a
                href="https://github.com/cnexans/UberDriverCompanion"
                target="_blank"
                rel="noopener noreferrer"
                className="text-green-400 hover:text-green-300 underline"
              >
                GitHub
              </a>
              .
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
