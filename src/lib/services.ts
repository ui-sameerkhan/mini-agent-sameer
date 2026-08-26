import { Service } from "../types/booking";

export const SERVICES: Service[] = [
  { id: "haircut", name: "Haircut", category: "Salon", price: 25, durationMinutes: 30 },
  { id: "hair-color", name: "Hair Coloring", category: "Salon", price: 60, durationMinutes: 90 },
  { id: "manicure", name: "Manicure", category: "Salon", price: 20, durationMinutes: 40 },
  { id: "facial", name: "Facial Treatment", category: "Salon", price: 45, durationMinutes: 60 },

  { id: "deep-clean", name: "Deep House Cleaning", category: "Home Cleaning", price: 80, durationMinutes: 150 },
  { id: "kitchen-clean", name: "Kitchen Cleaning", category: "Home Cleaning", price: 35, durationMinutes: 60 },
  { id: "carpet-clean", name: "Carpet Cleaning", category: "Home Cleaning", price: 40, durationMinutes: 45 },

  { id: "ac-repair", name: "AC Repair", category: "Repairs", price: 55, durationMinutes: 60 },
  { id: "plumbing", name: "Plumbing Fix", category: "Repairs", price: 50, durationMinutes: 45 },
  { id: "electrical", name: "Electrical Wiring Check", category: "Repairs", price: 45, durationMinutes: 40 },

  { id: "yoga", name: "Personal Yoga Session", category: "Wellness", price: 30, durationMinutes: 60 },
  { id: "massage", name: "Full Body Massage", category: "Wellness", price: 70, durationMinutes: 75 },
];

export const SERVICE_CATEGORIES = Array.from(
  new Set(SERVICES.map((service) => service.category)),
);
