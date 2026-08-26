import { useEffect, useState } from "react";
import ServiceCatalog from "../components/booking/ServiceCatalog";
import BookingCart from "../components/booking/BookingCart";
import BookingForm from "../components/booking/BookingForm";
import BookingHistory from "../components/booking/BookingHistory";
import { SERVICES } from "../lib/services";
import { loadBookings, saveBooking } from "../lib/bookings";
import { Booking, CartItem, CustomerDetails } from "../types/booking";

export default function BookingPage() {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [confirmation, setConfirmation] = useState<Booking | null>(null);

  useEffect(() => {
    setBookings(loadBookings());
  }, []);

  function toggleService(serviceId: string) {
    setCart((prev) => {
      const exists = prev.some((item) => item.service.id === serviceId);
      if (exists) return prev.filter((item) => item.service.id !== serviceId);
      const service = SERVICES.find((s) => s.id === serviceId)!;
      return [...prev, { service, quantity: 1 }];
    });
  }

  function updateQuantity(serviceId: string, quantity: number) {
    setCart((prev) =>
      prev.map((item) => (item.service.id === serviceId ? { ...item, quantity } : item)),
    );
  }

  function removeFromCart(serviceId: string) {
    setCart((prev) => prev.filter((item) => item.service.id !== serviceId));
  }

  function handleSubmit(customer: CustomerDetails) {
    const totalPrice = cart.reduce((sum, item) => sum + item.service.price * item.quantity, 0);
    const totalDurationMinutes = cart.reduce(
      (sum, item) => sum + item.service.durationMinutes * item.quantity,
      0,
    );

    const booking: Booking = {
      id: crypto.randomUUID(),
      createdAt: new Date().toISOString(),
      customer,
      items: cart,
      totalPrice,
      totalDurationMinutes,
    };

    const updated = saveBooking(booking);
    setBookings(updated);
    setConfirmation(booking);
    setCart([]);
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-800">Bulk Service Booking</h1>
        <p className="text-sm text-slate-500">
          Select multiple services and book them together in a single appointment slot.
        </p>
      </div>

      {confirmation && (
        <div className="mb-6 flex items-start justify-between rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-800">
          <div>
            <p className="font-semibold">Booking confirmed!</p>
            <p>
              {confirmation.items.length} service(s) booked for {confirmation.customer.date} at{" "}
              {confirmation.customer.time}. Total: ${confirmation.totalPrice}.
            </p>
          </div>
          <button
            onClick={() => setConfirmation(null)}
            className="text-green-700 hover:text-green-900"
            aria-label="Dismiss"
          >
            ✕
          </button>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <ServiceCatalog cart={cart} onToggle={toggleService} onQuantityChange={updateQuantity} />
        <div className="space-y-4">
          <BookingCart cart={cart} onRemove={removeFromCart} />
          <BookingForm disabled={cart.length === 0} onSubmit={handleSubmit} />
        </div>
      </div>

      <BookingHistory bookings={bookings} />
    </div>
  );
}
