export interface Service {
  id: string;
  name: string;
  category: string;
  price: number;
  durationMinutes: number;
}

export interface CartItem {
  service: Service;
  quantity: number;
}

export interface CustomerDetails {
  name: string;
  email: string;
  phone: string;
  state: string;
  district: string;
  date: string;
  time: string;
  notes: string;
}

/** One person/booking's place in a service+date group deal. */
export interface GroupJoin {
  bookingId: string;
  serviceId: string;
  date: string;
  joinOrder: number;
}

export interface BookingItem {
  service: Service;
  quantity: number;
  /** This item's position in the service+date group deal at the time it was booked. */
  joinOrder: number;
  /** Service price after the district multiplier, before the group-deal discount. */
  baseUnitPrice: number;
  /** Per-unit price locked in at the moment of booking. */
  pricePaidAtBooking: number;
}

export interface Booking {
  id: string;
  createdAt: string;
  customer: CustomerDetails;
  items: BookingItem[];
  totalPrice: number;
  totalDurationMinutes: number;
}
