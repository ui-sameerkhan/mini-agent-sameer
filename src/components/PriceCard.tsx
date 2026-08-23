import type { Coin, Quote } from "../types";

interface Props {
  coin: Coin;
  quote: Quote | null;
  loading: boolean;
}

export default function PriceCard({ coin, quote, loading }: Props) {
  const isUp = (quote?.changePct24h ?? 0) >= 0;

  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <div className="flex items-baseline justify-between">
        <div>
          <p className="text-sm text-slate-400">{coin.name}</p>
          <p className="text-3xl font-semibold">
            {loading || !quote ? "—" : `$${quote.price.toLocaleString(undefined, { maximumFractionDigits: 4 })}`}
          </p>
        </div>
        {quote && (
          <span className={`rounded-md px-2 py-1 text-sm font-medium ${isUp ? "bg-buy/15 text-buy" : "bg-sell/15 text-sell"}`}>
            {isUp ? "▲" : "▼"} {Math.abs(quote.changePct24h).toFixed(2)}% (24h)
          </span>
        )}
      </div>
      {quote && (
        <p className="mt-2 text-xs text-slate-500">
          Updated {new Date(quote.updatedAt).toLocaleTimeString()}
        </p>
      )}
    </div>
  );
}
