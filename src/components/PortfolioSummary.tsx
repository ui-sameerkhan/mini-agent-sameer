import type { Coin, Portfolio } from "../types";

interface Props {
  portfolio: Portfolio;
  livePrices: Record<string, number>;
  coins: Coin[];
  onReset: () => void;
}

export default function PortfolioSummary({ portfolio, livePrices, coins, onReset }: Props) {
  const coinById = new Map(coins.map((c) => [c.id, c]));
  const holdings = Object.values(portfolio.holdings);

  const holdingsValue = holdings.reduce((sum, h) => {
    const price = livePrices[h.coinId] ?? h.avgCost;
    return sum + price * h.amount;
  }, 0);

  const totalValue = portfolio.cash + holdingsValue;
  const investedCost = holdings.reduce((sum, h) => sum + h.avgCost * h.amount, 0);
  const unrealizedPnl = holdingsValue - investedCost;
  const startingCash = 10_000;
  const totalPnl = totalValue - startingCash;
  const totalPnlPct = (totalPnl / startingCash) * 100;

  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-400">Portfolio</p>
        <button onClick={onReset} className="text-xs text-slate-500 hover:text-slate-300">
          Reset
        </button>
      </div>

      <div className="mt-2 grid grid-cols-2 gap-3">
        <div>
          <p className="text-xs text-slate-500">Total value</p>
          <p className="text-xl font-semibold">${totalValue.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
        </div>
        <div>
          <p className="text-xs text-slate-500">Cash</p>
          <p className="text-xl font-semibold">${portfolio.cash.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
        </div>
        <div>
          <p className="text-xs text-slate-500">Total P&amp;L</p>
          <p className={`text-lg font-semibold ${totalPnl >= 0 ? "text-buy" : "text-sell"}`}>
            {totalPnl >= 0 ? "+" : ""}
            ${totalPnl.toLocaleString(undefined, { maximumFractionDigits: 2 })} ({totalPnlPct.toFixed(2)}%)
          </p>
        </div>
        <div>
          <p className="text-xs text-slate-500">Unrealized P&amp;L</p>
          <p className={`text-lg font-semibold ${unrealizedPnl >= 0 ? "text-buy" : "text-sell"}`}>
            {unrealizedPnl >= 0 ? "+" : ""}
            ${unrealizedPnl.toLocaleString(undefined, { maximumFractionDigits: 2 })}
          </p>
        </div>
      </div>

      {holdings.length > 0 && (
        <div className="mt-4 space-y-2">
          {holdings.map((h) => {
            const price = livePrices[h.coinId] ?? h.avgCost;
            const value = price * h.amount;
            const pnl = value - h.avgCost * h.amount;
            const pnlPct = h.avgCost > 0 ? (pnl / (h.avgCost * h.amount)) * 100 : 0;
            return (
              <div key={h.coinId} className="flex items-center justify-between text-sm">
                <span className="text-slate-300">
                  {coinById.get(h.coinId)?.symbol ?? h.symbol} · {h.amount.toFixed(6)}
                </span>
                <span className={pnl >= 0 ? "text-buy" : "text-sell"}>
                  {pnl >= 0 ? "+" : ""}
                  ${pnl.toFixed(2)} ({pnlPct.toFixed(1)}%)
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
