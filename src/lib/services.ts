import { Service } from "../types/booking";

export const SERVICES: Service[] = [
  { id: "haircut", name: "Haircut", category: "Salon", price: 250, durationMinutes: 30 },
  { id: "hair-color", name: "Hair Coloring", category: "Salon", price: 1500, durationMinutes: 90 },
  { id: "manicure", name: "Manicure", category: "Salon", price: 300, durationMinutes: 40 },
  { id: "facial", name: "Facial Treatment", category: "Salon", price: 800, durationMinutes: 60 },

  { id: "deep-clean", name: "Deep House Cleaning", category: "Home Cleaning", price: 2500, durationMinutes: 150 },
  { id: "kitchen-clean", name: "Kitchen Cleaning", category: "Home Cleaning", price: 900, durationMinutes: 60 },
  { id: "carpet-clean", name: "Carpet Cleaning", category: "Home Cleaning", price: 700, durationMinutes: 45 },

  { id: "ac-repair", name: "AC Repair", category: "Repairs", price: 600, durationMinutes: 60 },
  { id: "plumbing", name: "Plumbing Fix", category: "Repairs", price: 500, durationMinutes: 45 },
  { id: "electrical", name: "Electrical Wiring Check", category: "Repairs", price: 450, durationMinutes: 40 },

  { id: "yoga", name: "Personal Yoga Session", category: "Wellness", price: 400, durationMinutes: 60 },
  { id: "massage", name: "Full Body Massage", category: "Wellness", price: 1500, durationMinutes: 75 },

  { id: "mistri-labour", name: "Mistri (Labour)", category: "Labour & Domestic Help", price: 600, durationMinutes: 480 },
  { id: "mistri-carpenter", name: "Mistri (Carpenter)", category: "Labour & Domestic Help", price: 800, durationMinutes: 480 },
  { id: "kaamwali", name: "Kaamwali (Domestic Help)", category: "Labour & Domestic Help", price: 300, durationMinutes: 120 },
  { id: "cook", name: "Cook", category: "Labour & Domestic Help", price: 400, durationMinutes: 180 },
  { id: "helper", name: "Helper", category: "Labour & Domestic Help", price: 350, durationMinutes: 480 },
];

export const SERVICE_CATEGORIES = Array.from(
  new Set(SERVICES.map((service) => service.category)),
);
