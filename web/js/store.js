// Central in-memory store, fed by data.js's live listeners (started once in app.js after login).
// Feature views subscribe and re-render on change — simple pub/sub, no framework needed at this scale.
const state = {
  session: { isLoggedIn: false },
  workers: [],
  sites: [],
  todayAttendance: [],
  leaves: [],
  holidays: [],
  blocked: [],
  arrivals: [],
  announcement: null,
  myLinkedWorkerId: null,
};

const subscribers = new Set();

export function getState() {
  return state;
}

export function setState(patch) {
  Object.assign(state, patch);
  subscribers.forEach((fn) => fn(state));
}

export function subscribe(fn) {
  subscribers.add(fn);
  return () => subscribers.delete(fn);
}
