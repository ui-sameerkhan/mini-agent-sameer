import { rsi, sma } from "./indicators";
import type { Signal } from "../types";

const SHORT_PERIOD = 7;
const LONG_PERIOD = 21;
const RSI_PERIOD = 14;
const RSI_OVERSOLD = 30;
const RSI_OVERBOUGHT = 70;

export function computeSignal(prices: number[]): Signal {
  const smaShort = sma(prices, SHORT_PERIOD);
  const smaLong = sma(prices, LONG_PERIOD);
  const rsiValue = rsi(prices, RSI_PERIOD);

  const reasons: string[] = [];
  let score = 0;

  if (smaShort !== null && smaLong !== null) {
    if (smaShort > smaLong) {
      score += 1;
      reasons.push(`Short-term average (${smaShort.toFixed(2)}) is above long-term average (${smaLong.toFixed(2)}) — uptrend`);
    } else if (smaShort < smaLong) {
      score -= 1;
      reasons.push(`Short-term average (${smaShort.toFixed(2)}) is below long-term average (${smaLong.toFixed(2)}) — downtrend`);
    }
  }

  if (rsiValue !== null) {
    if (rsiValue < RSI_OVERSOLD) {
      score += 1;
      reasons.push(`RSI is ${rsiValue.toFixed(1)} — oversold, potential bounce`);
    } else if (rsiValue > RSI_OVERBOUGHT) {
      score -= 1;
      reasons.push(`RSI is ${rsiValue.toFixed(1)} — overbought, potential pullback`);
    } else {
      reasons.push(`RSI is ${rsiValue.toFixed(1)} — neutral zone`);
    }
  }

  let action: Signal["action"] = "HOLD";
  if (score >= 1) action = "BUY";
  else if (score <= -1) action = "SELL";

  const confidence = Math.min(100, Math.abs(score) * 40 + 30);

  return {
    action,
    confidence: reasons.length ? confidence : 0,
    reasons: reasons.length ? reasons : ["Not enough price history yet — keep tracking."],
    rsi: rsiValue,
    smaShort,
    smaLong,
  };
}
