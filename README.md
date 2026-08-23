# TradeSense — Real-Time Market Tracker

A React + TypeScript + TailwindCSS trading assistant that tracks live crypto
prices, generates buy/sell/hold signals from technical indicators, and lets
you paper-trade to practice profit/loss management — with no real money and
no exchange account required.

---

## 🚀 Features

- **Real-time price tracking** for BTC, ETH, SOL, XRP, DOGE, ADA (polled every 30s from CoinGecko, no API key needed)
- **Buy / Sell / Hold signals** computed from:
  - RSI(14) — flags oversold (<30) and overbought (>70) conditions
  - SMA(7) vs SMA(21) crossover — flags short-term trend direction
  - Each recommendation lists the reasons and a confidence score
- **Live price chart** of the last 7 days plus new ticks as they arrive
- **Paper-trading portfolio** — starts with $10,000 virtual cash
  - Buy/sell at the live market price
  - Tracks holdings, average cost, realized cash, and trade history
  - Shows **unrealized P&L** per holding and **total P&L** for the portfolio
  - Persists in the browser (`localStorage`) between sessions
- **Educational disclaimer** on every signal — this is a learning tool, not financial advice

---

## 🛠️ Tech Stack

| Tool / Library | Purpose |
|----------------|---------|
| React + TypeScript | UI and component structure |
| Vite | Dev server and build |
| TailwindCSS | Styling |
| Recharts | Price chart |
| CoinGecko public API | Live prices and historical data |

---

## Getting started

```bash
npm install
npm run dev
```

Open the printed local URL in your browser. No API key or account setup is
required — the app talks directly to CoinGecko's public API from the browser.

```bash
npm run build    # type-check + production build
npm run preview  # preview the production build
```

---

## How the signal works

For the selected coin's recent price history:

1. **RSI(14)** below 30 → oversold (bullish tilt); above 70 → overbought (bearish tilt)
2. **SMA(7) vs SMA(21)** — short average above long average → uptrend (bullish tilt); below → downtrend (bearish tilt)
3. The tilts are combined into a `BUY`, `SELL`, or `HOLD` recommendation with a confidence score and plain-language reasons

This is a simple, transparent rule-based strategy meant for learning how
indicators and signals work — not a guarantee of profit. Always do your own
research before trading with real money.

---

## Project structure

```
src/
  api/coingecko.ts       CoinGecko API client + watchlist
  lib/indicators.ts      SMA / RSI calculations
  lib/signals.ts         Buy/Sell/Hold decision logic
  hooks/useMarketData.ts Live price polling + history
  hooks/usePortfolio.ts  Paper-trading state (localStorage)
  components/            Dashboard UI (price card, chart, signal, trade panel, portfolio, history)
  App.tsx                Wires everything together
```
