import { GroupJoin } from "../types/booking";

const STORAGE_KEY = "service-group-joins";

function groupKey(serviceId: string, date: string): string {
  return `${serviceId}__${date}`;
}

function loadAllGroups(): Record<string, GroupJoin[]> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Record<string, GroupJoin[]>) : {};
  } catch {
    return {};
  }
}

function saveAllGroups(data: Record<string, GroupJoin[]>): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

/** How many people are currently in this service+date group. */
export function getGroupSize(serviceId: string, date: string): number {
  if (!date) return 0;
  return (loadAllGroups()[groupKey(serviceId, date)] ?? []).length;
}

/** Registers a new booking into the service+date group and returns its join position. */
export function addGroupJoin(serviceId: string, date: string, bookingId: string): GroupJoin {
  const data = loadAllGroups();
  const key = groupKey(serviceId, date);
  const existing = data[key] ?? [];
  const join: GroupJoin = { bookingId, serviceId, date, joinOrder: existing.length + 1 };
  data[key] = [...existing, join];
  saveAllGroups(data);
  return join;
}
