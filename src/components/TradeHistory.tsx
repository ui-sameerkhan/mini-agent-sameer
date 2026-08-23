import type { Trade } from "../types";

export default function TradeHistory({ trades }: { trades: Trade[] }) {
  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <p className="mb-3 text-sm text-slate-400">Trade history</p>
      {trades.length === 0 ? (
        <p className="text-sm text-slate-500">No trades yet — place your first paper trade above.</p>
      ) : (
        <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
          {trades.map((t) => (
            <div key={t.id} className="flex items-center justify-between text-sm">
              <span className={`w-12 font-semibold ${t.side === "BUY" ? "text-buy" : "text-sell"}`}>{t.side}</span>
              <span className="flex-1 text-slate-300">
                {t.amount.toFixed(6)} {t.symbol} @ ${t.price.toLocaleString(undefined, { maximumFractionDigits: 2 })}
              </span>
              <span className="text-xs text-slate-500">{new Date(t.timestamp).toLocaleTimeString()}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
