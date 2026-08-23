import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { PricePoint } from "../types";

export default function PriceChart({ history }: { history: PricePoint[] }) {
  const data = history.map((p) => ({
    time: new Date(p.time).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    price: p.price,
  }));

  return (
    <div className="rounded-xl bg-slate-900 p-5">
      <p className="mb-3 text-sm text-slate-400">Price history</p>
      <div className="h-64 w-full">
        {data.length > 1 ? (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data}>
              <XAxis dataKey="time" stroke="#64748b" fontSize={11} tickLine={false} minTickGap={40} />
              <YAxis
                stroke="#64748b"
                fontSize={11}
                tickLine={false}
                domain={["auto", "auto"]}
                tickFormatter={(v: number) => `$${v.toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
                width={70}
              />
              <Tooltip
                contentStyle={{ background: "#0f172a", border: "1px solid #1e293b", borderRadius: 8 }}
                labelStyle={{ color: "#94a3b8" }}
                formatter={(value: number) => [`$${value.toLocaleString()}`, "Price"]}
              />
              <Line type="monotone" dataKey="price" stroke="#6366f1" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex h-full items-center justify-center text-sm text-slate-500">
            Loading chart…
          </div>
        )}
      </div>
    </div>
  );
}
