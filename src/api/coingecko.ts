import type { Coin, PricePoint, Quote } from "../types";

const BASE_URL = "https://api.coingecko.com/api/v3";

export const WATCHLIST: Coin[] = [
  { id: "bitcoin", symbol: "BTC", name: "Bitcoin" },
  { id: "ethereum", symbol: "ETH", name: "Ethereum" },
  { id: "solana", symbol: "SOL", name: "Solana" },
  { id: "ripple", symbol: "XRP", name: "XRP" },
  { id: "dogecoin", symbol: "DOGE", name: "Dogecoin" },
  { id: "cardano", symbol: "ADA", name: "Cardano" },
];

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Market data request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

export async function getQuote(coinId: string): Promise<Quote> {
  const url = `${BASE_URL}/simple/price?ids=${coinId}&vs_currencies=usd&include_24hr_change=true`;
  const data = await fetchJson<Record<string, { usd: number; usd_24h_change: number }>>(url);
  const entry = data[coinId];
  if (!entry) throw new Error(`No quote found for ${coinId}`);
  return {
    price: entry.usd,
    changePct24h: entry.usd_24h_change ?? 0,
    updatedAt: Date.now(),
  };
}

export async function getHistory(coinId: string, days = 7): Promise<PricePoint[]> {
  const url = `${BASE_URL}/coins/${coinId}/market_chart?vs_currency=usd&days=${days}`;
  const data = await fetchJson<{ prices: [number, number][] }>(url);
  return data.prices.map(([time, price]) => ({ time, price }));
}
