export interface Coin {
  id: string;
  symbol: string;
  name: string;
}

export interface PricePoint {
  time: number;
  price: number;
}

export interface Quote {
  price: number;
  changePct24h: number;
  updatedAt: number;
}

export type SignalAction = "BUY" | "SELL" | "HOLD";

export interface Signal {
  action: SignalAction;
  confidence: number;
  reasons: string[];
  rsi: number | null;
  smaShort: number | null;
  smaLong: number | null;
}

export interface Trade {
  id: string;
  coinId: string;
  symbol: string;
  side: "BUY" | "SELL";
  price: number;
  amount: number;
  total: number;
  timestamp: number;
}

export interface Holding {
  coinId: string;
  symbol: string;
  amount: number;
  avgCost: number;
}

export interface Portfolio {
  cash: number;
  holdings: Record<string, Holding>;
  trades: Trade[];
}
