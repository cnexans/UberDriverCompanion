export function ArchDiagram() {
  const boxes = [
    {
      label: "AccessibilityService",
      sub: "Detecta eventos de Uber Driver",
      col: "border-blue-500/30 bg-blue-500/5",
    },
    {
      label: "takeScreenshot()",
      sub: "Captura pantalla cada 100ms",
      col: "border-purple-500/30 bg-purple-500/5",
    },
    {
      label: "ML Kit OCR",
      sub: "Extrae texto de la imagen",
      col: "border-amber-500/30 bg-amber-500/5",
    },
    {
      label: "TripParser",
      sub: "Parsea precio, distancias, tipo",
      col: "border-cyan-500/30 bg-cyan-500/5",
    },
    {
      label: "OverlayService",
      sub: "Muestra metricas en pantalla",
      col: "border-green-500/30 bg-green-500/5",
    },
    {
      label: "Room DB",
      sub: "Guarda historial local",
      col: "border-neutral-500/30 bg-neutral-500/5",
    },
  ];

  return (
    <div className="p-8 rounded-xl bg-neutral-900/30 border border-neutral-800">
      <h3 className="text-sm font-semibold text-neutral-500 uppercase tracking-wider mb-6 text-center">
        Arquitectura
      </h3>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        {boxes.map((box, i) => (
          <div key={i} className="relative">
            <div
              className={`p-3 rounded-lg border text-center ${box.col} h-full`}
            >
              <div className="text-xs font-semibold mb-1">{box.label}</div>
              <div className="text-[10px] text-neutral-500">{box.sub}</div>
            </div>
            {i < boxes.length - 1 && (
              <div className="hidden lg:block absolute top-1/2 -right-3 text-neutral-600 text-xs">
                →
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
