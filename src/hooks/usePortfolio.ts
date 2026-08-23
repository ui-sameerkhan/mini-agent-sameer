import { useCallback, useEffect, useState } from "react";
import type { Portfolio, Trade } from "../types";

const STORAGE_KEY = "tradesense.portfolio.v1";
const STARTING_CASH = 10_000;

function loadPortfolio(): Portfolio {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as Portfolio;
  } catch {
    // ignore corrupt storage, fall back to a fresh portfolio
  }
  return { cash: STARTING_CASH, holdings: {}, trades: [] };
}

export function usePortfolio() {
  const [portfolio, setPortfolio] = useState<Portfolio>(loadPortfolio);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(portfolio));
  }, [portfolio]);

  const buy = useCallback((coinId: string, symbol: string, price: number, amount: number) => {
    setPortfolio((prev) => {
      const total = price * amount;
      if (total > prev.cash || amount <= 0) return prev;
      const existing = prev.holdings[coinId];
      const newAmount = (existing?.amount ?? 0) + amount;
      const newAvgCost = existing
        ? (existing.avgCost * existing.amount + total) / newAmount
        : price;
      const trade: Trade = {
        id: crypto.randomUUID(),
        coinId,
        symbol,
        side: "BUY",
        price,
        amount,
        total,
        timestamp: Date.now(),
      };
      return {
        cash: prev.cash - total,
        holdings: {
          ...prev.holdings,
          [coinId]: { coinId, symbol, amount: newAmount, avgCost: newAvgCost },
        },
        trades: [trade, ...prev.trades],
      };
    });
  }, []);

  const sell = useCallback((coinId: string, symbol: string, price: number, amount: number) => {
    setPortfolio((prev) => {
      const existing = prev.holdings[coinId];
      if (!existing || amount <= 0 || amount > existing.amount) return prev;
      const total = price * amount;
      const remaining = existing.amount - amount;
      const holdings = { ...prev.holdings };
      if (remaining <= 0) delete holdings[coinId];
      else holdings[coinId] = { ...existing, amount: remaining };
      const trade: Trade = {
        id: crypto.randomUUID(),
        coinId,
        symbol,
        side: "SELL",
        price,
        amount,
        total,
        timestamp: Date.now(),
      };
      return {
        cash: prev.cash + total,
        holdings,
        trades: [trade, ...prev.trades],
      };
    });
  }, []);

  const reset = useCallback(() => {
    setPortfolio({ cash: STARTING_CASH, holdings: {}, trades: [] });
  }, []);

  return { portfolio, buy, sell, reset };
}
