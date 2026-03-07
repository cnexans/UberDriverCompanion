interface MetricCardProps {
  label: string;
  value: string;
  description: string;
  good: string;
  bad: string;
  color: "green" | "yellow" | "red";
}

export function MetricCard({
  label,
  value,
  description,
  good,
  bad,
  color,
}: MetricCardProps) {
  const colorMap = {
    green: "text-green-400 border-green-500/30 bg-green-500/5",
    yellow: "text-yellow-400 border-yellow-500/30 bg-yellow-500/5",
    red: "text-red-400 border-red-500/30 bg-red-500/5",
  };

  return (
    <div className="p-6 rounded-xl bg-neutral-900/50 border border-neutral-800 hover:border-neutral-700 transition-colors">
      <div className="text-sm text-neutral-500 mb-1">{label}</div>
      <div className={`text-4xl font-bold mb-3 ${colorMap[color].split(" ")[0]}`}>
        {value}
      </div>
      <p className="text-sm text-neutral-400 leading-relaxed mb-4">
        {description}
      </p>
      <div className="flex gap-3 text-xs">
        <span className="px-2 py-1 rounded bg-green-500/10 text-green-400 border border-green-500/20">
          {good}
        </span>
        <span className="px-2 py-1 rounded bg-red-500/10 text-red-400 border border-red-500/20">
          {bad}
        </span>
      </div>
    </div>
  );
}
