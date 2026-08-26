interface Props {
  date: string;
  time: string;
  onDateChange: (date: string) => void;
  onTimeChange: (time: string) => void;
}

export default function BookingSlotPicker({ date, time, onDateChange, onTimeChange }: Props) {
  const inputClass =
    "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

  return (
    <div className="mb-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="mb-1 text-sm font-semibold text-slate-800">1. Pick your booking date</h3>
      <p className="mb-3 text-xs text-slate-500">
        🎉 Group Deal: everyone who already booked a service for this date gets a lower price
        every time someone new joins. Book early to catch the biggest drop!
      </p>
      <div className="grid max-w-xs grid-cols-2 gap-2">
        <input
          type="date"
          value={date}
          onChange={(e) => onDateChange(e.target.value)}
          className={inputClass}
        />
        <input
          type="time"
          value={time}
          onChange={(e) => onTimeChange(e.target.value)}
          className={inputClass}
        />
      </div>
    </div>
  );
}
