import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Eliminacion de Datos — Pana Uber",
};

export default function DataDeletionPage() {
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
          Eliminacion de Datos
        </h1>
        <p className="text-neutral-500 mb-8">
          Ultima actualizacion: 9 de marzo de 2026
        </p>

        <div className="space-y-6 text-neutral-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              1. Datos almacenados
            </h2>
            <p>
              Pana Uber (en adelante &quot;la App&quot;) almacena todos los datos
              de forma local en el dispositivo del usuario. Esto incluye el
              historial de viajes analizados, metricas calculadas y
              configuraciones de la aplicacion. Ningun dato personal se envia ni
              se almacena en servidores externos.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              2. Como eliminar tus datos
            </h2>
            <p>
              Dado que todos los datos se almacenan localmente en tu dispositivo,
              podes eliminarlos completamente de dos formas:
            </p>
            <ul className="list-disc list-inside mt-4 space-y-3 text-neutral-400">
              <li>
                <strong className="text-white">Borrar datos de la app:</strong>{" "}
                Anda a Configuracion &gt; Aplicaciones &gt; Pana Uber &gt;
                Almacenamiento &gt; Borrar datos. Esto elimina toda la
                informacion almacenada por la App sin necesidad de desinstalarla.
              </li>
              <li>
                <strong className="text-white">Desinstalar la app:</strong>{" "}
                Al desinstalar Pana Uber de tu dispositivo, todos los datos
                locales asociados se eliminan automaticamente por el sistema
                operativo Android.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              3. Datos en servidores externos
            </h2>
            <p>
              La App no recopila ni almacena datos personales en ningun servidor
              externo. Por lo tanto, no existe informacion del usuario que deba
              ser eliminada de nuestro lado. La unica herramienta de terceros
              utilizada es PostHog para analitica anonima del sitio web, la cual
              no recopila datos identificables del usuario.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              4. Confirmacion de eliminacion
            </h2>
            <p>
              Una vez que borras los datos de la app o la desinstalas, la
              eliminacion es inmediata y permanente. No se conservan copias de
              seguridad ni datos residuales en ningun servidor.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              5. Contacto
            </h2>
            <p>
              Si tenes preguntas sobre la eliminacion de datos, podes
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
