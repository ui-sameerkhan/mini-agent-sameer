export function sma(values: number[], period: number): number | null {
  if (values.length < period) return null;
  const slice = values.slice(values.length - period);
  return slice.reduce((sum, v) => sum + v, 0) / period;
}

export function stdDev(values: number[], period: number): number | null {
  if (values.length < period) return null;
  const slice = values.slice(values.length - period);
  const mean = slice.reduce((sum, v) => sum + v, 0) / period;
  const variance = slice.reduce((sum, v) => sum + (v - mean) ** 2, 0) / period;
  return Math.sqrt(variance);
}

export function emaSeries(values: number[], period: number): (number | null)[] {
  const result: (number | null)[] = new Array(values.length).fill(null);
  if (values.length < period) return result;
  const k = 2 / (period + 1);
  const seed = values.slice(0, period).reduce((sum, v) => sum + v, 0) / period;
  result[period - 1] = seed;
  for (let i = period; i < values.length; i++) {
    const prev = result[i - 1] as number;
    result[i] = values[i] * k + prev * (1 - k);
  }
  return result;
}

export function ema(values: number[], period: number): number | null {
  const series = emaSeries(values, period);
  return series.length > 0 ? series[series.length - 1] : null;
}

/** Wilder's RSI — the standard smoothing method used by most trading platforms. */
export function rsi(values: number[], period = 14): number | null {
  if (values.length < period + 1) return null;
  let avgGain = 0;
  let avgLoss = 0;
  for (let i = 1; i <= period; i++) {
    const diff = values[i] - values[i - 1];
    if (diff >= 0) avgGain += diff;
    else avgLoss -= diff;
  }
  avgGain /= period;
  avgLoss /= period;
  for (let i = period + 1; i < values.length; i++) {
    const diff = values[i] - values[i - 1];
    const gain = diff > 0 ? diff : 0;
    const loss = diff < 0 ? -diff : 0;
    avgGain = (avgGain * (period - 1) + gain) / period;
    avgLoss = (avgLoss * (period - 1) + loss) / period;
  }
  if (avgLoss === 0) return 100;
  const rs = avgGain / avgLoss;
  return 100 - 100 / (1 + rs);
}

export interface MacdResult {
  macd: number | null;
  signal: number | null;
  histogram: number | null;
}

export function macd(values: number[], fastPeriod = 12, slowPeriod = 26, signalPeriod = 9): MacdResult {
  const fastSeries = emaSeries(values, fastPeriod);
  const slowSeries = emaSeries(values, slowPeriod);
  const macdSeries: number[] = [];
  for (let i = 0; i < values.length; i++) {
    const f = fastSeries[i];
    const s = slowSeries[i];
    if (f !== null && s !== null) macdSeries.push(f - s);
  }
  if (macdSeries.length < signalPeriod) {
    return { macd: macdSeries.length ? macdSeries[macdSeries.length - 1] : null, signal: null, histogram: null };
  }
  const signalSeries = emaSeries(macdSeries, signalPeriod);
  const macdLine = macdSeries[macdSeries.length - 1];
  const signalLine = signalSeries[signalSeries.length - 1];
  return {
    macd: macdLine,
    signal: signalLine,
    histogram: signalLine !== null ? macdLine - signalLine : null,
  };
}

export interface BollingerBands {
  upper: number | null;
  middle: number | null;
  lower: number | null;
}

export function bollingerBands(values: number[], period = 20, mult = 2): BollingerBands {
  const middle = sma(values, period);
  const dev = stdDev(values, period);
  if (middle === null || dev === null) return { upper: null, middle: null, lower: null };
  return { upper: middle + mult * dev, middle, lower: middle - mult * dev };
}
