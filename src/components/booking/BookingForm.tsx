import { FormEvent, useState } from "react";
import { CustomerDetails } from "../../types/booking";

interface Props {
  disabled: boolean;
  onSubmit: (details: CustomerDetails) => void;
}

const EMPTY_DETAILS: CustomerDetails = {
  name: "",
  email: "",
  phone: "",
  date: "",
  time: "",
  notes: "",
};

export default function BookingForm({ disabled, onSubmit }: Props) {
  const [details, setDetails] = useState<CustomerDetails>(EMPTY_DETAILS);

  function update<K extends keyof CustomerDetails>(key: K, value: CustomerDetails[K]) {
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
      <h3 className="text-sm font-semibold text-slate-800">Your Details</h3>
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
        <div className="grid grid-cols-2 gap-2">
          <input
            required
            type="date"
            value={details.date}
            onChange={(e) => update("date", e.target.value)}
            className={inputClass}
          />
          <input
            required
            type="time"
            value={details.time}
            onChange={(e) => update("time", e.target.value)}
            className={inputClass}
          />
        </div>
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
