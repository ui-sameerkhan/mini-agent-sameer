import { useMemo, useState } from "react";
import { WATCHLIST } from "./api/coingecko";
import { useMarketData } from "./hooks/useMarketData";
import { usePortfolio } from "./hooks/usePortfolio";
import { computeSignal } from "./lib/signals";
import SymbolSelector from "./components/SymbolSelector";
import PriceCard from "./components/PriceCard";
import SignalBadge from "./components/SignalBadge";
import PriceChart from "./components/PriceChart";
import IndicatorsPanel from "./components/IndicatorsPanel";
import TradePanel from "./components/TradePanel";
import PortfolioSummary from "./components/PortfolioSummary";
import TradeHistory from "./components/TradeHistory";

export default function App() {
  const [selectedId, setSelectedId] = useState(WATCHLIST[0].id);
  const coin = WATCHLIST.find((c) => c.id === selectedId)!;

  const { quote, history, error, loading } = useMarketData(selectedId);
  const { portfolio, buy, sell, reset } = usePortfolio();

  const signal = useMemo(() => computeSignal(history.map((p) => p.price)), [history]);

  const livePrices = useMemo(() => {
    const map: Record<string, number> = {};
    if (quote) map[selectedId] = quote.price;
    return map;
  }, [quote, selectedId]);

  return (
    <div className="min-h-screen bg-slate-950 px-4 py-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-6">
          <h1 className="text-2xl font-bold">TradeSense</h1>
          <p className="text-sm text-slate-400">
            Real-time crypto tracking with buy/sell signals and a paper-trading portfolio.
          </p>
        </header>

        <div className="mb-6">
          <SymbolSelector coins={WATCHLIST} selected={selectedId} onSelect={setSelectedId} />
        </div>

        {error && (
          <div className="mb-6 rounded-lg bg-sell/10 px-4 py-3 text-sm text-sell">
            {error} — retrying automatically.
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <PriceCard coin={coin} quote={quote} loading={loading} />
          <SignalBadge signal={signal} />
          <div className="md:col-span-2">
            <PriceChart history={history} />
          </div>
          <IndicatorsPanel signal={signal} />
          <TradePanel
            coin={coin}
            quote={quote}
            holding={portfolio.holdings[selectedId]}
            cash={portfolio.cash}
            onBuy={(amount) => quote && buy(selectedId, coin.symbol, quote.price, amount)}
            onSell={(amount) => quote && sell(selectedId, coin.symbol, quote.price, amount)}
          />
          <div className="md:col-span-2">
            <PortfolioSummary portfolio={portfolio} livePrices={livePrices} coins={WATCHLIST} onReset={reset} />
          </div>
          <div className="md:col-span-2">
            <TradeHistory trades={portfolio.trades} />
          </div>
        </div>

        <footer className="mt-8 text-center text-xs text-slate-600">
          Prices from CoinGecko · Paper trading only, no real money involved.
        </footer>
      </div>
    </div>
  );
}
