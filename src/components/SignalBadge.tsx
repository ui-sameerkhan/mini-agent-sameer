import type { Signal } from "../types";

const STYLES: Record<Signal["action"], string> = {
  BUY: "bg-buy/15 text-buy border-buy/40",
  SELL: "bg-sell/15 text-sell border-sell/40",
  HOLD: "bg-hold/15 text-hold border-hold/40",
};

export default function SignalBadge({ signal }: { signal: Signal }) {
  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-400">Recommendation</p>
        <span className={`rounded-md border px-3 py-1 text-sm font-bold ${STYLES[signal.action]}`}>
          {signal.action}
        </span>
      </div>
      <p className="mt-2 text-xs text-slate-500">Confidence: {signal.confidence}%</p>
      <ul className="mt-3 space-y-1 text-sm text-slate-300">
        {signal.reasons.map((reason, i) => (
          <li key={i} className="flex gap-2">
            <span className="text-slate-600">•</span>
            <span>{reason}</span>
          </li>
        ))}
      </ul>
      <p className="mt-3 text-xs text-slate-600">
        Educational signal only — not financial advice. Always do your own research.
      </p>
    </div>
  );
}
