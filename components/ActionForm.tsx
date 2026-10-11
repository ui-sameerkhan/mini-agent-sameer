"use client";
import { startTransition, useActionState } from "react";
import type { State } from "@/app/actions";

/** Submits via onSubmit (not <form action>) so React 19 does not wipe the user's input when the server returns an error. */
export function useKeepInput(run: (fd: FormData) => void) {
  return (e: React.FormEvent<HTMLFormElement>) => { e.preventDefault(); const fd = new FormData(e.currentTarget); startTransition(() => run(fd)); };
}

export default function ActionForm({ action, children, className, encType }: { action: (s: State, fd: FormData) => Promise<State>; children: React.ReactNode; className?: string; encType?: string }) {
  const [state, run, pending] = useActionState(action, undefined);
  return (
    <form onSubmit={useKeepInput(run)} className={className} encType={encType} aria-busy={pending}>
      {state?.error && <div className="err" role="alert">{state.error}</div>}
      {state?.ok && <div className="okmsg" role="status">{state.ok}</div>}
      {children}
    </form>
  );
}
