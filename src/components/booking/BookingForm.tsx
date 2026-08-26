import { FormEvent, useState } from "react";

export interface ContactDetails {
  name: string;
  email: string;
  phone: string;
  notes: string;
}

interface Props {
  disabled: boolean;
  disabledReason?: string;
  onSubmit: (details: ContactDetails) => void;
}

const EMPTY_DETAILS: ContactDetails = {
  name: "",
  email: "",
  phone: "",
  notes: "",
};

export default function BookingForm({ disabled, disabledReason, onSubmit }: Props) {
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
          pattern="[6-9]\d{9}"
          title="Enter a 10-digit Indian mobile number"
          onChange={(e) => update("phone", e.target.value)}
          className={`${inputClass} sm:col-span-2`}
        />
      </div>
      <textarea
        placeholder="Notes (optional)"
        value={details.notes}
        onChange={(e) => update("notes", e.target.value)}
        rows={2}
        className={inputClass}
      />
      {disabled && disabledReason && (
        <p className="text-xs text-amber-600">{disabledReason}</p>
      )}
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
