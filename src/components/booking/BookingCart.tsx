import { CartItem } from "../../types/booking";
import { formatINR } from "../../lib/currency";

interface Props {
  cart: CartItem[];
  priceMultiplier: number;
  onRemove: (serviceId: string) => void;
}

export default function BookingCart({ cart, priceMultiplier, onRemove }: Props) {
  const adjustedPrice = (item: CartItem) => Math.round(item.service.price * priceMultiplier);
  const totalPrice = cart.reduce((sum, item) => sum + adjustedPrice(item) * item.quantity, 0);
  const totalDuration = cart.reduce(
    (sum, item) => sum + item.service.durationMinutes * item.quantity,
    0,
  );

  if (cart.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center text-sm text-slate-500">
        No services selected yet. Pick services from the left to add them here.
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="mb-1 text-sm font-semibold text-slate-800">Selected Services</h3>
      <p className="mb-3 text-xs text-slate-500">
        This is the price for your selected date. It may drop further if more people book the
        same date after you.
      </p>
      <ul className="space-y-2">
        {cart.map((item) => (
          <li key={item.service.id} className="flex items-center justify-between text-sm">
            <span>
              {item.service.name} <span className="text-slate-400">× {item.quantity}</span>
            </span>
            <div className="flex items-center gap-3">
              <span className="text-slate-600">{formatINR(adjustedPrice(item) * item.quantity)}</span>
              <button
                type="button"
                onClick={() => onRemove(item.service.id)}
                className="text-xs font-medium text-red-500 hover:text-red-600"
              >
                Remove
              </button>
            </div>
          </li>
        ))}
      </ul>
      <div className="mt-4 space-y-1 border-t border-slate-200 pt-3 text-sm">
        <div className="flex justify-between text-slate-600">
          <span>Total duration</span>
          <span>{totalDuration} min</span>
        </div>
        <div className="flex justify-between text-base font-semibold text-slate-800">
          <span>Total</span>
          <span>{formatINR(totalPrice)}</span>
        </div>
      </div>
    </div>
  );
}
