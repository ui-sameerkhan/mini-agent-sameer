import type { Signal } from "../types";

export default function IndicatorsPanel({ signal }: { signal: Signal }) {
  const rows: [string, string][] = [
    ["RSI (14)", signal.rsi !== null ? signal.rsi.toFixed(1) : "—"],
    ["SMA short (7)", signal.smaShort !== null ? `$${signal.smaShort.toFixed(2)}` : "—"],
    ["SMA long (21)", signal.smaLong !== null ? `$${signal.smaLong.toFixed(2)}` : "—"],
  ];

  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <p className="mb-3 text-sm text-slate-400">Indicators</p>
      <dl className="space-y-2">
        {rows.map(([label, value]) => (
          <div key={label} className="flex items-center justify-between text-sm">
            <dt className="text-slate-400">{label}</dt>
            <dd className="font-medium text-slate-200">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
