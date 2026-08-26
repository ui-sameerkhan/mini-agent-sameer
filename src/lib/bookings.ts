import { Booking } from "../types/booking";

const STORAGE_KEY = "bulk-service-bookings";

export function loadBookings(): Booking[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Booking[]) : [];
  } catch {
    return [];
  }
}

export function saveBooking(booking: Booking): Booking[] {
  const bookings = [booking, ...loadBookings()];
  localStorage.setItem(STORAGE_KEY, JSON.stringify(bookings));
  return bookings;
}
