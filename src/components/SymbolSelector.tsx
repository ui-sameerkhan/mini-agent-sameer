import type { Coin } from "../types";

interface Props {
  coins: Coin[];
  selected: string;
  onSelect: (coinId: string) => void;
}

export default function SymbolSelector({ coins, selected, onSelect }: Props) {
  return (
    <div className="flex flex-wrap gap-2">
      {coins.map((coin) => (
        <button
          key={coin.id}
          onClick={() => onSelect(coin.id)}
          className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
            selected === coin.id
              ? "bg-indigo-500 text-white"
              : "bg-slate-800 text-slate-300 hover:bg-slate-700"
          }`}
        >
          {coin.symbol}
        </button>
      ))}
    </div>
  );
}
