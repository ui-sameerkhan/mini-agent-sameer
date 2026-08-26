import { Booking } from "../../types/booking";
import { getGroupSize } from "../../lib/groupJoins";
import { groupMemberPrice } from "../../lib/groupPricing";

function liveTotalFor(booking: Booking): number {
  return booking.items.reduce((sum, item) => {
    const groupSize = getGroupSize(item.service.id, booking.customer.date);
    const livePrice = groupMemberPrice(item.service.price, groupSize, item.joinOrder);
    return sum + livePrice * item.quantity;
  }, 0);
}

export default function BookingHistory({ bookings }: { bookings: Booking[] }) {
  if (bookings.length === 0) return null;

  return (
    <div className="mt-8">
      <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
        Booking History
      </h3>
      <div className="space-y-3">
        {bookings.map((booking) => {
          const liveTotal = liveTotalFor(booking);
          const saved = Math.round((booking.totalPrice - liveTotal) * 100) / 100;

          return (
            <div key={booking.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between text-sm">
                <span className="font-medium text-slate-800">{booking.customer.name}</span>
                <span className="text-slate-500">
                  {booking.customer.date} at {booking.customer.time}
                </span>
              </div>
              <p className="mt-1 text-xs text-slate-500">
                {booking.items.map((item) => `${item.service.name} ×${item.quantity}`).join(", ")}
              </p>
              <div className="mt-1 flex items-center gap-2">
                {saved > 0 ? (
                  <>
                    <span className="text-sm text-slate-400 line-through">${booking.totalPrice}</span>
                    <span className="text-sm font-semibold text-emerald-600">${liveTotal} now</span>
                    <span className="text-xs text-emerald-600">
                      (saved ${saved} as more people joined!)
                    </span>
                  </>
                ) : (
                  <span className="text-sm font-semibold text-slate-700">${booking.totalPrice}</span>
                )}
                <span className="text-sm text-slate-400">· {booking.totalDurationMinutes} min</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
