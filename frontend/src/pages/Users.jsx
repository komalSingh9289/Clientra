import { useState } from "react";
import { Plus, Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Field, Input, Select } from "@/components/ui/input";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { PageHeader, Loading, ErrorState } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { usersApi } from "@/services/api";
import { useAuth } from "@/context/AuthContext";
import { fmtDate, isEmail } from "@/lib/utils";

/** UC-13 Manage user accounts (Admin only): create, edit, assign role, deactivate. */
export default function Users() {
  const { user: me } = useAuth();
  const { toast } = useToast();
  const users = useApi(() => usersApi.list(), []);
  const roles = useApi(() => usersApi.roles(), []).data || [];
  const [dlg, setDlg] = useState(null); // null | { user?: existing }
  const [f, setF] = useState({}); const [err, setErr] = useState("");
  if (users.loading && !users.data) return <Loading />;
  if (users.error) return <ErrorState error={users.error} />;

  const openDlg = (u) => { setDlg({ user: u }); setErr(""); setF(u ? { fullName: u.fullName, email: u.email, roleId: u.roleId } : { fullName: "", email: "", roleId: 3, password: "" }); };
  const save = async () => {
    if (!f.fullName.trim() || !isEmail(f.email)) return setErr("Enter a name and a valid email.");
    if (!dlg.user && (f.password || "").length < 8) return setErr("Password must be at least 8 characters.");
    try { dlg.user ? await usersApi.update(dlg.user.userId, f) : await usersApi.create(f); toast({ title: dlg.user ? "User updated" : "User created" }); setDlg(null); users.reload(); }
    catch (e) { setErr(e.message); }
  };
  const toggle = async (u) => { await usersApi.setActive(u.userId, !u.active); toast({ title: u.active ? "User deactivated" : "User activated" }); users.reload(); };

  return (
    <>
      <PageHeader title="Users" description="Create accounts, assign roles and deactivate people who leave."><Button size="sm" onClick={() => openDlg(null)}><Plus />Add user</Button></PageHeader>
      <Card><Table><THead><TR><TH>Name</TH><TH>Role</TH><TH className="hidden md:table-cell">Last login</TH><TH>Status</TH><TH className="w-36" /></TR></THead>
        <TBody>{users.data.map((u) => (
          <TR key={u.userId}><TD className="font-medium">{u.fullName}<p className="text-xs font-normal text-muted-foreground">{u.email}</p></TD><TD>{u.role}</TD><TD className="hidden md:table-cell text-muted-foreground">{fmtDate(u.lastLogin)}</TD>
            <TD><Badge variant={u.active ? "success" : "secondary"}>{u.active ? "Active" : "Deactivated"}</Badge></TD>
            <TD><div className="flex justify-end gap-1"><Button size="icon" variant="ghost" aria-label={`Edit ${u.fullName}`} onClick={() => openDlg(u)}><Pencil /></Button>
              <Button size="sm" variant="outline" disabled={u.userId === me.userId} onClick={() => toggle(u)}>{u.active ? "Deactivate" : "Activate"}</Button></div></TD></TR>))}</TBody></Table></Card>
      <Dialog open={!!dlg} onOpenChange={() => setDlg(null)}><DialogContent><DialogHeader><DialogTitle>{dlg?.user ? "Edit user" : "Add user"}</DialogTitle></DialogHeader>
        <div className="grid gap-3">
          <Field label="Full name"><Input value={f.fullName || ""} onChange={(e) => setF({ ...f, fullName: e.target.value })} /></Field>
          <Field label="Email"><Input type="email" value={f.email || ""} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
          <Field label="Role"><Select value={f.roleId} onChange={(e) => setF({ ...f, roleId: e.target.value })}>{roles.map((r) => <option key={r.roleId} value={r.roleId}>{r.roleName}</option>)}</Select></Field>
          {!dlg?.user && <Field label="Temporary password" hint="At least 8 characters. The user can change it after signing in."><Input type="password" value={f.password || ""} onChange={(e) => setF({ ...f, password: e.target.value })} /></Field>}
          {err && <p className="text-sm text-destructive">{err}</p>}
        </div><DialogFooter><Button variant="outline" onClick={() => setDlg(null)}>Cancel</Button><Button onClick={save}>Save user</Button></DialogFooter></DialogContent></Dialog>
    </>
  );
}
