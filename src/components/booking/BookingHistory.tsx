import { Booking } from "../../types/booking";

export default function BookingHistory({ bookings }: { bookings: Booking[] }) {
  if (bookings.length === 0) return null;

  return (
    <div className="mt-8">
      <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
        Booking History
      </h3>
      <div className="space-y-3">
        {bookings.map((booking) => (
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
            <p className="mt-1 text-sm font-semibold text-slate-700">
              ${booking.totalPrice} · {booking.totalDurationMinutes} min
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
