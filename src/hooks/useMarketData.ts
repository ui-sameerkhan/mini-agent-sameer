import { useEffect, useRef, useState } from "react";
import { getHistory, getQuote } from "../api/coingecko";
import type { PricePoint, Quote } from "../types";

const POLL_INTERVAL_MS = 30_000;

export function useMarketData(coinId: string) {
  const [quote, setQuote] = useState<Quote | null>(null);
  const [history, setHistory] = useState<PricePoint[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const intervalRef = useRef<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setQuote(null);
    setHistory([]);

    async function loadHistory() {
      try {
        const points = await getHistory(coinId, 7);
        if (!cancelled) setHistory(points);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load history");
      }
    }

    async function pollQuote() {
      try {
        const q = await getQuote(coinId);
        if (cancelled) return;
        setQuote(q);
        setError(null);
        setHistory((prev) => {
          const next = [...prev, { time: q.updatedAt, price: q.price }];
          return next.length > 300 ? next.slice(next.length - 300) : next;
        });
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load quote");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadHistory().then(pollQuote);
    intervalRef.current = window.setInterval(pollQuote, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      if (intervalRef.current) window.clearInterval(intervalRef.current);
    };
  }, [coinId]);

  return { quote, history, error, loading };
}
