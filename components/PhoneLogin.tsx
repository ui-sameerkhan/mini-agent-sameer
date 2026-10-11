"use client";
import { useActionState } from "react";
import { useKeepInput } from "./ActionForm";
import { requestOtp, verifyOtp, type State } from "@/app/actions";

export default function PhoneLogin({ labels }: { labels: { phone: string; send: string; enter: string; verify: string } }) {
  const [a, ask, p1] = useActionState<State, FormData>(requestOtp, undefined);
  const [b, check, p2] = useActionState<State, FormData>(verifyOtp, undefined);
  const phone = b?.phone || a?.phone;
  const sent = (b ? b.sent : a?.sent) && phone;
  const err = b?.error || a?.error;
  return (
    <div className="stack">
      {err && <div className="err" role="alert">{err}</div>}
      {!sent ? (
        <form onSubmit={useKeepInput(ask)} className="stack">
          <label htmlFor="phone">{labels.phone}</label>
          <input id="phone" name="phone" type="tel" inputMode="tel" autoComplete="tel" placeholder="05X XXX XXXX" required style={{ fontSize: "1.3rem" }} />
          <button className="btn big" disabled={p1}>{labels.send}</button>
        </form>
      ) : (
        <form onSubmit={useKeepInput(check)} className="stack">
          <input type="hidden" name="phone" value={phone} />
          <label htmlFor="code">{labels.enter} ({phone})</label>
          <input id="code" name="code" inputMode="numeric" autoComplete="one-time-code" pattern="\d{6}" maxLength={6} required style={{ fontSize: "1.6rem", letterSpacing: ".4em", textAlign: "center" }} />
          {a?.devCode && <div className="safe">DEV MODE — no SMS provider set. Your code is <b>{a.devCode}</b></div>}
          <button className="btn big" disabled={p2}>{labels.verify}</button>
        </form>
      )}
    </div>
  );
}
