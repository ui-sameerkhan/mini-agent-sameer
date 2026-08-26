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
  date: string;
  time: string;
  notes: string;
}

export interface Booking {
  id: string;
  createdAt: string;
  customer: CustomerDetails;
  items: CartItem[];
  totalPrice: number;
  totalDurationMinutes: number;
}
