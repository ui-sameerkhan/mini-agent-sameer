import { NavLink } from "react-router-dom";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive
      ? "bg-brand-600 text-white"
      : "text-slate-600 hover:bg-slate-200"
  }`;

export default function Nav() {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="max-w-5xl mx-auto flex items-center justify-between px-4 py-3">
        <span className="font-semibold text-slate-800">Mini-Agent Suite</span>
        <nav className="flex gap-2">
          <NavLink to="/" end className={linkClass}>
            Chat
          </NavLink>
          <NavLink to="/booking" className={linkClass}>
            Bulk Booking
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
