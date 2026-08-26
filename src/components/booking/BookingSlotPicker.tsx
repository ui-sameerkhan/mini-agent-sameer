import { INDIAN_STATES, INDIAN_UNION_TERRITORIES } from "../../lib/indianStates";
import { getDistrictsForState, getDistrictTier, TIER_LABEL } from "../../lib/districts";

interface Props {
  date: string;
  time: string;
  state: string;
  district: string;
  onDateChange: (date: string) => void;
  onTimeChange: (time: string) => void;
  onStateChange: (state: string) => void;
  onDistrictChange: (district: string) => void;
}

export default function BookingSlotPicker({
  date,
  time,
  state,
  district,
  onDateChange,
  onTimeChange,
  onStateChange,
  onDistrictChange,
}: Props) {
  const inputClass =
    "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

  const districts = getDistrictsForState(state);
  const tier = state && district ? getDistrictTier(state, district) : null;

  return (
    <div className="mb-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="mb-1 text-sm font-semibold text-slate-800">1. Your location &amp; booking date</h3>
      <p className="mb-3 text-xs text-slate-500">
        Prices are adjusted for your area, and 🎉 Group Deal: everyone who already booked a
        service for this date gets a lower price every time someone new joins. Book early to
        catch the biggest drop!
      </p>
      <div className="grid max-w-2xl gap-2 sm:grid-cols-2">
        <select
          value={state}
          onChange={(e) => {
            onStateChange(e.target.value);
            onDistrictChange("");
          }}
          className={inputClass}
        >
          <option value="" disabled>
            Select state
          </option>
          <optgroup label="States">
            {INDIAN_STATES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </optgroup>
          <optgroup label="Union Territories">
            {INDIAN_UNION_TERRITORIES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </optgroup>
        </select>

        <select
          value={district}
          disabled={!state}
          onChange={(e) => onDistrictChange(e.target.value)}
          className={inputClass}
        >
          <option value="" disabled>
            {state ? "Select district" : "Select a state first"}
          </option>
          {districts.map((d) => (
            <option key={d.name} value={d.name}>
              {d.name}
            </option>
          ))}
        </select>

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
      {tier && (
        <p className="mt-2 text-xs font-medium text-brand-700">{TIER_LABEL[tier]} for {district}</p>
      )}
    </div>
  );
}
