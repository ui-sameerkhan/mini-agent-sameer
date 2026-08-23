import { useState } from "react";
import type { Coin, Holding, Quote } from "../types";

interface Props {
  coin: Coin;
  quote: Quote | null;
  holding: Holding | undefined;
  cash: number;
  onBuy: (amount: number) => void;
  onSell: (amount: number) => void;
}

export default function TradePanel({ coin, quote, holding, cash, onBuy, onSell }: Props) {
  const [amountStr, setAmountStr] = useState("");
  const amount = Number(amountStr);
  const price = quote?.price ?? 0;
  const cost = amount * price;
  const canBuy = amount > 0 && cost <= cash && price > 0;
  const canSell = amount > 0 && !!holding && amount <= holding.amount;

  function handleBuy() {
    if (!canBuy) return;
    onBuy(amount);
    setAmountStr("");
  }

  function handleSell() {
    if (!canSell) return;
    onSell(amount);
    setAmountStr("");
  }

  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <p className="mb-3 text-sm text-slate-400">Paper trade {coin.symbol}</p>
      <div className="flex gap-2">
        <input
          type="number"
          min="0"
          step="any"
          value={amountStr}
          onChange={(e) => setAmountStr(e.target.value)}
          placeholder={`Amount of ${coin.symbol}`}
          className="w-full rounded-lg bg-slate-800 px-3 py-2 text-sm outline-none ring-1 ring-slate-700 focus:ring-indigo-500"
        />
      </div>
      {amount > 0 && (
        <p className="mt-2 text-xs text-slate-500">≈ ${cost.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
      )}
      <div className="mt-3 flex gap-2">
        <button
          onClick={handleBuy}
          disabled={!canBuy}
          className="flex-1 rounded-lg bg-buy/90 py-2 text-sm font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Buy
        </button>
        <button
          onClick={handleSell}
          disabled={!canSell}
          className="flex-1 rounded-lg bg-sell/90 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          Sell
        </button>
      </div>
      <p className="mt-3 text-xs text-slate-500">
        Cash available: ${cash.toLocaleString(undefined, { maximumFractionDigits: 2 })}
        {holding && ` · Holding: ${holding.amount.toFixed(6)} ${coin.symbol}`}
      </p>
    </div>
  );
}
