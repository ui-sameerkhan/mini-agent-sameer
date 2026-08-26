import { FormEvent, useState } from "react";
import { INDIAN_STATES, INDIAN_UNION_TERRITORIES } from "../../lib/indianStates";

export interface ContactDetails {
  name: string;
  email: string;
  phone: string;
  state: string;
  notes: string;
}

interface Props {
  disabled: boolean;
  onSubmit: (details: ContactDetails) => void;
}

const EMPTY_DETAILS: ContactDetails = {
  name: "",
  email: "",
  phone: "",
  state: "",
  notes: "",
};

export default function BookingForm({ disabled, onSubmit }: Props) {
  const [details, setDetails] = useState<ContactDetails>(EMPTY_DETAILS);

  function update<K extends keyof ContactDetails>(key: K, value: ContactDetails[K]) {
    setDetails((prev) => ({ ...prev, [key]: value }));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit(details);
    setDetails(EMPTY_DETAILS);
  }

  const inputClass =
    "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

  return (
    <form onSubmit={handleSubmit} className="space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-800">2. Your Details</h3>
      <div className="grid gap-3 sm:grid-cols-2">
        <input
          required
          placeholder="Full name"
          value={details.name}
          onChange={(e) => update("name", e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="email"
          placeholder="Email"
          value={details.email}
          onChange={(e) => update("email", e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="tel"
          placeholder="Phone number"
          value={details.phone}
          onChange={(e) => update("phone", e.target.value)}
          className={inputClass}
        />
        <select
          required
          value={details.state}
          onChange={(e) => update("state", e.target.value)}
          className={inputClass}
        >
          <option value="" disabled>
            Select state
          </option>
          <optgroup label="States">
            {INDIAN_STATES.map((state) => (
              <option key={state} value={state}>
                {state}
              </option>
            ))}
          </optgroup>
          <optgroup label="Union Territories">
            {INDIAN_UNION_TERRITORIES.map((state) => (
              <option key={state} value={state}>
                {state}
              </option>
            ))}
          </optgroup>
        </select>
      </div>
      <textarea
        placeholder="Notes (optional)"
        value={details.notes}
        onChange={(e) => update("notes", e.target.value)}
        rows={2}
        className={inputClass}
      />
      <button
        type="submit"
        disabled={disabled}
        className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Confirm Bulk Booking
      </button>
    </form>
  );
}
