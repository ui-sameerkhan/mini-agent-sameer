export type DistrictTier = 1 | 2 | 3;

export interface District {
  name: string;
  tier: DistrictTier;
}

/** Tier 1 = metro/major city (costlier), 2 = standard city, 3 = smaller town/rural (cheaper). */
export const TIER_MULTIPLIER: Record<DistrictTier, number> = {
  1: 1.3,
  2: 1.0,
  3: 0.8,
};

export const TIER_LABEL: Record<DistrictTier, string> = {
  1: "Metro pricing (+30%)",
  2: "Standard pricing",
  3: "Smaller town pricing (-20%)",
};

const CURATED_DISTRICTS: Record<string, District[]> = {
  "Maharashtra": [
    { name: "Mumbai", tier: 1 },
    { name: "Pune", tier: 1 },
    { name: "Thane", tier: 2 },
    { name: "Nagpur", tier: 2 },
    { name: "Nashik", tier: 3 },
  ],
  "Delhi (NCT)": [
    { name: "New Delhi", tier: 1 },
    { name: "South Delhi", tier: 1 },
    { name: "North Delhi", tier: 2 },
  ],
  "Karnataka": [
    { name: "Bengaluru Urban", tier: 1 },
    { name: "Mysuru", tier: 2 },
    { name: "Mangaluru", tier: 3 },
  ],
  "Tamil Nadu": [
    { name: "Chennai", tier: 1 },
    { name: "Coimbatore", tier: 2 },
    { name: "Madurai", tier: 3 },
  ],
  "West Bengal": [
    { name: "Kolkata", tier: 1 },
    { name: "Howrah", tier: 2 },
    { name: "Darjeeling", tier: 3 },
  ],
  "Telangana": [
    { name: "Hyderabad", tier: 1 },
    { name: "Warangal", tier: 3 },
  ],
  "Gujarat": [
    { name: "Ahmedabad", tier: 1 },
    { name: "Surat", tier: 2 },
    { name: "Vadodara", tier: 2 },
    { name: "Rajkot", tier: 3 },
  ],
  "Haryana": [
    { name: "Gurugram", tier: 1 },
    { name: "Faridabad", tier: 2 },
    { name: "Panipat", tier: 3 },
  ],
  "Uttar Pradesh": [
    { name: "Gautam Buddh Nagar (Noida)", tier: 1 },
    { name: "Lucknow", tier: 2 },
    { name: "Kanpur Nagar", tier: 2 },
    { name: "Varanasi", tier: 3 },
    { name: "Agra", tier: 3 },
  ],
  "Rajasthan": [
    { name: "Jaipur", tier: 2 },
    { name: "Jodhpur", tier: 3 },
    { name: "Udaipur", tier: 3 },
  ],
  "Madhya Pradesh": [
    { name: "Bhopal", tier: 2 },
    { name: "Indore", tier: 2 },
    { name: "Gwalior", tier: 3 },
  ],
  "Kerala": [
    { name: "Ernakulam (Kochi)", tier: 2 },
    { name: "Thiruvananthapuram", tier: 2 },
    { name: "Kozhikode", tier: 3 },
  ],
  "Punjab": [
    { name: "Ludhiana", tier: 2 },
    { name: "Amritsar", tier: 2 },
    { name: "Jalandhar", tier: 3 },
  ],
  "Bihar": [
    { name: "Patna", tier: 2 },
    { name: "Gaya", tier: 3 },
  ],
  "Andhra Pradesh": [
    { name: "Visakhapatnam", tier: 2 },
    { name: "Vijayawada", tier: 2 },
    { name: "Guntur", tier: 3 },
  ],
  "Odisha": [
    { name: "Khordha (Bhubaneswar)", tier: 2 },
    { name: "Cuttack", tier: 3 },
  ],
  "Assam": [{ name: "Kamrup Metropolitan (Guwahati)", tier: 2 }],
  "Chhattisgarh": [{ name: "Raipur", tier: 2 }],
  "Jharkhand": [
    { name: "Ranchi", tier: 2 },
    { name: "East Singhbhum (Jamshedpur)", tier: 2 },
  ],
  "Uttarakhand": [{ name: "Dehradun", tier: 2 }],
  "Himachal Pradesh": [{ name: "Shimla", tier: 3 }],
  "Goa": [
    { name: "North Goa", tier: 2 },
    { name: "South Goa", tier: 3 },
  ],
  "Chandigarh": [{ name: "Chandigarh", tier: 2 }],
  "Jammu and Kashmir": [
    { name: "Srinagar", tier: 3 },
    { name: "Jammu", tier: 3 },
  ],
  "Ladakh": [{ name: "Leh", tier: 3 }],
  "Puducherry": [{ name: "Puducherry", tier: 3 }],
};

export function getDistrictsForState(state: string): District[] {
  if (!state) return [];
  const curated = CURATED_DISTRICTS[state] ?? [];
  return [...curated, { name: `Other district in ${state}`, tier: 3 }];
}

export function getDistrictTier(state: string, districtName: string): DistrictTier {
  const match = getDistrictsForState(state).find((d) => d.name === districtName);
  return match?.tier ?? 2;
}

export function applyDistrictPricing(basePrice: number, tier: DistrictTier): number {
  return Math.round(basePrice * TIER_MULTIPLIER[tier]);
}
