import { useEffect, useState } from "react";
import ServiceCatalog from "../components/booking/ServiceCatalog";
import BookingCart from "../components/booking/BookingCart";
import BookingForm, { ContactDetails } from "../components/booking/BookingForm";
import BookingHistory from "../components/booking/BookingHistory";
import BookingSlotPicker from "../components/booking/BookingSlotPicker";
import { SERVICES } from "../lib/services";
import { loadBookings, saveBooking } from "../lib/bookings";
import { addGroupJoin } from "../lib/groupJoins";
import { Booking, BookingItem, CartItem } from "../types/booking";

function todayISODate(): string {
  return new Date().toISOString().slice(0, 10);
}

export default function BookingPage() {
  const [date, setDate] = useState(todayISODate());
  const [time, setTime] = useState("10:00");
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

  function handleSubmit(contact: ContactDetails) {
    const bookingId = crypto.randomUUID();

    const items: BookingItem[] = cart.map((cartItem) => {
      const join = addGroupJoin(cartItem.service.id, date, bookingId);
      return {
        service: cartItem.service,
        quantity: cartItem.quantity,
        joinOrder: join.joinOrder,
        pricePaidAtBooking: cartItem.service.price,
      };
    });

    const totalPrice = items.reduce(
      (sum, item) => sum + item.pricePaidAtBooking * item.quantity,
      0,
    );
    const totalDurationMinutes = items.reduce(
      (sum, item) => sum + item.service.durationMinutes * item.quantity,
      0,
    );

    const booking: Booking = {
      id: bookingId,
      createdAt: new Date().toISOString(),
      customer: { name: contact.name, email: contact.email, phone: contact.phone, date, time, notes: contact.notes },
      items,
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

      <BookingSlotPicker date={date} time={time} onDateChange={setDate} onTimeChange={setTime} />

      {confirmation && (
        <div className="mb-6 flex items-start justify-between rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-800">
          <div>
            <p className="font-semibold">Booking confirmed!</p>
            <p>
              {confirmation.items.length} service(s) booked for {confirmation.customer.date} at{" "}
              {confirmation.customer.time}. Total: ${confirmation.totalPrice}. Check Booking
              History below — your price may drop further as more people join this date.
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
        <ServiceCatalog cart={cart} date={date} onToggle={toggleService} onQuantityChange={updateQuantity} />
        <div className="space-y-4">
          <BookingCart cart={cart} onRemove={removeFromCart} />
          <BookingForm disabled={cart.length === 0} onSubmit={handleSubmit} />
        </div>
      </div>

      <BookingHistory bookings={bookings} />
    </div>
  );
}
