import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Field, Input } from "@/components/ui/input";
import { useAuth } from "@/context/AuthContext";

/** UC-01 Login. Demo accounts are listed so each role can be tried; remove the block when the backend is live. */
const DEMO = [
  ["Administrator", "admin@clientra.demo", "admin123"],
  ["Sales Manager", "manager@clientra.demo", "manager123"],
  ["Sales Representative", "rep@clientra.demo", "rep123"],
];

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState(""); const [password, setPassword] = useState("");
  const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setError("");
    try { await login(email, password); } catch (err) { setError(err.message); setBusy(false); }
  };
  return (
    <div className="grid min-h-screen place-items-center p-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <span className="mx-auto mb-3 grid size-10 place-items-center rounded-lg bg-primary text-lg font-semibold text-primary-foreground">C</span>
          <h1 className="text-2xl font-semibold">Sign in to Clientra</h1>
          <p className="mt-1 text-sm text-muted-foreground">Track leads, follow up on time and let AI do the busywork.</p>
        </div>
        <form onSubmit={submit} className="space-y-4 rounded-lg border bg-card p-5">
          <Field label="Email"><Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required /></Field>
          <Field label="Password"><Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required /></Field>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <Button type="submit" className="w-full" disabled={busy}>{busy ? "Signing in…" : "Sign in"}</Button>
        </form>
        <div className="mt-4 rounded-lg border border-dashed p-3">
          <p className="mb-2 text-xs font-medium text-muted-foreground">Demo accounts (select to fill)</p>
          {DEMO.map(([r, e, p]) => (
            <button key={r} type="button" onClick={() => { setEmail(e); setPassword(p); }} className="flex w-full justify-between rounded-md px-2 py-1.5 text-left text-sm hover:bg-muted">
              <span>{r}</span><span className="text-muted-foreground">{e}</span>
            </button>))}
        </div>
      </div>
    </div>
  );
}
