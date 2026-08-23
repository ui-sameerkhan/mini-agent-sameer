import { bollingerBands, ema, macd, rsi } from "./indicators";
import type { Signal } from "../types";

const EMA_SHORT = 12;
const EMA_LONG = 26;
const RSI_PERIOD = 14;
const RSI_OVERSOLD = 30;
const RSI_OVERBOUGHT = 70;
const BB_PERIOD = 20;

// A signal only fires when indicators agree — one bullish or bearish reading
// alone isn't enough. This is a confluence model: trend (EMA) and momentum
// (MACD) carry the most weight, RSI and Bollinger Bands confirm at extremes.
const ACTION_THRESHOLD = 0.5;

export function computeSignal(prices: number[]): Signal {
  const emaShort = ema(prices, EMA_SHORT);
  const emaLong = ema(prices, EMA_LONG);
  const { macd: macdLine, signal: macdSignal } = macd(prices);
  const rsiValue = rsi(prices, RSI_PERIOD);
  const { upper: bbUpper, lower: bbLower } = bollingerBands(prices, BB_PERIOD);
  const price = prices.length ? prices[prices.length - 1] : null;

  const reasons: string[] = [];
  let score = 0;
  let maxScore = 0;

  // Trend — EMA12 vs EMA26 (weight 2)
  if (emaShort !== null && emaLong !== null) {
    maxScore += 2;
    if (emaShort > emaLong) {
      score += 2;
      reasons.push(`Trend is up — EMA12 ($${emaShort.toFixed(2)}) is above EMA26 ($${emaLong.toFixed(2)})`);
    } else {
      score -= 2;
      reasons.push(`Trend is down — EMA12 ($${emaShort.toFixed(2)}) is below EMA26 ($${emaLong.toFixed(2)})`);
    }
  }

  // Momentum — MACD line vs signal line (weight 2)
  if (macdLine !== null && macdSignal !== null) {
    maxScore += 2;
    if (macdLine > macdSignal) {
      score += 2;
      reasons.push("MACD is above its signal line — bullish momentum");
    } else {
      score -= 2;
      reasons.push("MACD is below its signal line — bearish momentum");
    }
  }

  // Oscillator — RSI extremes (weight 1)
  if (rsiValue !== null) {
    maxScore += 1;
    if (rsiValue < RSI_OVERSOLD) {
      score += 1;
      reasons.push(`RSI is ${rsiValue.toFixed(1)} — oversold, momentum favors a bounce`);
    } else if (rsiValue > RSI_OVERBOUGHT) {
      score -= 1;
      reasons.push(`RSI is ${rsiValue.toFixed(1)} — overbought, momentum favors a pullback`);
    } else {
      reasons.push(`RSI is ${rsiValue.toFixed(1)} — neutral zone`);
    }
  }

  // Volatility / mean reversion — Bollinger Bands (weight 1)
  if (price !== null && bbUpper !== null && bbLower !== null) {
    maxScore += 1;
    if (price <= bbLower) {
      score += 1;
      reasons.push("Price is at/below the lower Bollinger Band — stretched to the downside");
    } else if (price >= bbUpper) {
      score -= 1;
      reasons.push("Price is at/above the upper Bollinger Band — stretched to the upside");
    } else {
      reasons.push("Price is within its normal Bollinger Band range");
    }
  }

  const normalized = maxScore > 0 ? score / maxScore : 0;

  let action: Signal["action"] = "HOLD";
  if (normalized >= ACTION_THRESHOLD) action = "BUY";
  else if (normalized <= -ACTION_THRESHOLD) action = "SELL";

  const confidence = maxScore > 0 ? Math.round(Math.abs(normalized) * 100) : 0;

  return {
    action,
    confidence,
    reasons: reasons.length ? reasons : ["Not enough price history yet — keep tracking."],
    rsi: rsiValue,
    emaShort,
    emaLong,
    macd: macdLine,
    macdSignal,
    bollingerUpper: bbUpper,
    bollingerLower: bbLower,
  };
}
