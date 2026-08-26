import { SERVICES, SERVICE_CATEGORIES } from "../../lib/services";
import { CartItem } from "../../types/booking";

interface Props {
  cart: CartItem[];
  onToggle: (serviceId: string) => void;
  onQuantityChange: (serviceId: string, quantity: number) => void;
}

export default function ServiceCatalog({ cart, onToggle, onQuantityChange }: Props) {
  const quantityFor = (serviceId: string) =>
    cart.find((item) => item.service.id === serviceId)?.quantity ?? 0;

  return (
    <div className="space-y-6">
      {SERVICE_CATEGORIES.map((category) => (
        <div key={category}>
          <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
            {category}
          </h3>
          <div className="grid gap-3 sm:grid-cols-2">
            {SERVICES.filter((service) => service.category === category).map((service) => {
              const quantity = quantityFor(service.id);
              const selected = quantity > 0;
              return (
                <div
                  key={service.id}
                  className={`rounded-xl border p-4 shadow-sm transition-colors ${
                    selected ? "border-brand-500 bg-brand-50" : "border-slate-200 bg-white"
                  }`}
                >
                  <label className="flex cursor-pointer items-start gap-3">
                    <input
                      type="checkbox"
                      checked={selected}
                      onChange={() => onToggle(service.id)}
                      className="mt-1 h-4 w-4 accent-brand-600"
                    />
                    <div className="flex-1">
                      <p className="text-sm font-medium text-slate-800">{service.name}</p>
                      <p className="text-xs text-slate-500">
                        ${service.price} · {service.durationMinutes} min
                      </p>
                    </div>
                  </label>
                  {selected && (
                    <div className="mt-3 flex items-center gap-2 pl-7">
                      <span className="text-xs text-slate-500">Qty</span>
                      <button
                        type="button"
                        onClick={() => onQuantityChange(service.id, Math.max(1, quantity - 1))}
                        className="h-6 w-6 rounded-full border border-slate-300 text-sm leading-none hover:bg-slate-100"
                      >
                        −
                      </button>
                      <span className="w-5 text-center text-sm">{quantity}</span>
                      <button
                        type="button"
                        onClick={() => onQuantityChange(service.id, quantity + 1)}
                        className="h-6 w-6 rounded-full border border-slate-300 text-sm leading-none hover:bg-slate-100"
                      >
                        +
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
