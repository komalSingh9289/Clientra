import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { PageHeader } from "@/components/shared/ui-bits";
import { authApi } from "@/services/api";
import { useAuth } from "@/context/AuthContext";

/** My account: every role can change their own password (POST /auth/change-password). */
export default function Account() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [f, setF] = useState({ current: "", next: "", again: "" });
  const [err, setErr] = useState("");
  const [saving, setSaving] = useState(false);

  const save = async (e) => {
    e.preventDefault();
    setErr("");
    if (f.next.length < 8) return setErr("New password must be at least 8 characters.");
    if (f.next !== f.again) return setErr("The new passwords do not match.");
    setSaving(true);
    try {
      await authApi.changePassword(f.current, f.next);
      setF({ current: "", next: "", again: "" });
      toast({ title: "Password changed" });
    } catch (ex) { setErr(ex.message); } finally { setSaving(false); }
  };

  return (
    <>
      <PageHeader title="My account" description={`${user.fullName} · ${user.email} · ${user.role}`} />
      <Card className="max-w-md">
        <CardHeader><CardTitle>Change password</CardTitle><CardDescription>Use at least 8 characters.</CardDescription></CardHeader>
        <CardContent>
          <form onSubmit={save} className="space-y-3">
            <Input type="password" placeholder="Current password" autoComplete="current-password" value={f.current} onChange={(e) => setF({ ...f, current: e.target.value })} />
            <Input type="password" placeholder="New password" autoComplete="new-password" value={f.next} onChange={(e) => setF({ ...f, next: e.target.value })} />
            <Input type="password" placeholder="Repeat new password" autoComplete="new-password" value={f.again} onChange={(e) => setF({ ...f, again: e.target.value })} />
            {err && <p className="text-sm text-destructive">{err}</p>}
            <Button type="submit" disabled={saving || !f.current}>{saving ? "Saving…" : "Change password"}</Button>
          </form>
        </CardContent>
      </Card>
    </>
  );
}
