import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Terminos y Condiciones — Pana Uber",
};

export default function TermsPage() {
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
          Terminos y Condiciones
        </h1>
        <p className="text-neutral-500 mb-8">
          Ultima actualizacion: 9 de marzo de 2026
        </p>

        <div className="space-y-6 text-neutral-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              1. Aceptacion de los terminos
            </h2>
            <p>
              Al descargar, instalar o utilizar Pana Uber (en adelante
              &quot;la App&quot;), aceptas estos terminos y condiciones en su
              totalidad. Si no estas de acuerdo con alguno de estos terminos, no
              debes utilizar la App.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              2. Naturaleza del software
            </h2>
            <p>
              La App se proporciona &quot;tal cual&quot; (&quot;as is&quot;), sin
              garantias de ningun tipo, ya sean expresas o implicitas, incluyendo
              pero no limitandose a garantias de comerciabilidad, adecuacion para
              un proposito particular o no infraccion.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              3. Limitacion de responsabilidad
            </h2>
            <p>
              Los desarrolladores de la App no se hacen responsables por ningun
              dano directo, indirecto, incidental, especial o consecuente que
              resulte del uso o la imposibilidad de uso de la App, incluyendo
              pero no limitandose a:
            </p>
            <ul className="list-disc list-inside mt-2 space-y-1 text-neutral-400">
              <li>Perdidas economicas derivadas de decisiones tomadas con base en la informacion mostrada por la App</li>
              <li>Errores en el calculo de metricas o en el reconocimiento optico de caracteres (OCR)</li>
              <li>Interrupciones del servicio o incompatibilidades con actualizaciones de Uber Driver</li>
              <li>Cualquier consecuencia derivada del uso de la App mientras se conduce</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              4. Uso bajo tu propia responsabilidad
            </h2>
            <p>
              El usuario es el unico responsable del uso que haga de la App y de
              las decisiones que tome basandose en la informacion que esta
              proporciona. La App es una herramienta de asistencia y no
              reemplaza el juicio del conductor.
            </p>
            <p className="mt-2">
              El usuario es responsable de cumplir con todas las leyes de
              transito y regulaciones aplicables mientras utiliza la App.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              5. Modelo de pago
            </h2>
            <p>
              Cualquier pago asociado a la App tiene caracter de{" "}
              <strong className="text-white">donativo voluntario</strong> para
              apoyar a los desarrolladores en el trabajo de mantener la App
              funcionando. El pago no constituye un canon, licencia o
              contraprestacion que otorgue derechos adicionales sobre el
              software, ni garantiza soporte tecnico, actualizaciones futuras o
              disponibilidad continuada del servicio.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              6. Propiedad intelectual
            </h2>
            <p>
              La App es software de codigo abierto distribuido bajo la licencia
              MIT. El codigo fuente esta disponible en{" "}
              <a
                href="https://github.com/cnexans/UberDriverCompanion"
                target="_blank"
                rel="noopener noreferrer"
                className="text-green-400 hover:text-green-300 underline"
              >
                GitHub
              </a>
              . &quot;Uber&quot; es una marca registrada de Uber Technologies,
              Inc. La App no esta afiliada, asociada ni respaldada por Uber
              Technologies, Inc.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              7. Servicio de accesibilidad
            </h2>
            <p>
              La App utiliza el Servicio de Accesibilidad de Android para
              funcionar. El usuario debe otorgar este permiso de forma voluntaria
              y consciente. La App utiliza este permiso exclusivamente para leer
              la informacion de viajes mostrada en la pantalla de Uber Driver.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              8. Modificaciones
            </h2>
            <p>
              Nos reservamos el derecho de modificar estos terminos en cualquier
              momento. Las modificaciones seran efectivas al momento de su
              publicacion en esta pagina. El uso continuado de la App despues de
              cualquier modificacion constituye la aceptacion de los nuevos
              terminos.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">
              9. Contacto
            </h2>
            <p>
              Para consultas sobre estos terminos, podes contactarnos a traves
              de nuestro repositorio en{" "}
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
