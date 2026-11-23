import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Field, Input, Select } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { useApi } from "@/hooks/useApi";
import { leadsApi, stagesApi, usersApi } from "@/services/api";
import { LEAD_SOURCES } from "@/services/constants";
import { useAuth } from "@/context/AuthContext";
import { isEmail } from "@/lib/utils";

/**
 * Add / Edit lead (UC-02). Client-side validation (email format, required fields);
 * duplicate detection comes from the API as error.code === "DUPLICATE" and the user may override.
 * Pass `lead` to edit, omit to create.
 */
export default function LeadFormDialog({ open, onOpenChange, lead, onSaved }) {
  const { user, can } = useAuth();
  const { toast } = useToast();
  const stages = useApi(() => stagesApi.list(), []).data || [];
  const users = useApi(() => usersApi.assignable(), []).data || [];
  const blank = { fullName: "", email: "", phone: "", company: "", source: "Website", stageId: 1, assignedTo: user.userId };
  const [f, setF] = useState(blank);
  const [errors, setErrors] = useState({});
  const [dupe, setDupe] = useState(null);
  const [saving, setSaving] = useState(false);
  useEffect(() => { if (open) { setF(lead ? { ...lead } : blank); setErrors({}); setDupe(null); } /* eslint-disable-next-line */ }, [open, lead]);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const validate = () => {
    const e = {};
    if (!f.fullName.trim()) e.fullName = "Enter the lead's full name.";
    if (!isEmail(f.email)) e.email = "Enter a valid email, like name@company.com.";
    if (f.phone && f.phone.replace(/\D/g, "").length < 10) e.phone = "Phone number needs at least 10 digits.";
    setErrors(e); return !Object.keys(e).length;
  };
  const save = async (force = false) => {
    if (!validate()) return;
    setSaving(true);
    try {
      const saved = lead ? await leadsApi.update(lead.leadId, f) : await leadsApi.create(f, { force });
      toast({ title: lead ? "Lead updated" : "Lead added" }); onSaved?.(saved); onOpenChange(false);
    } catch (err) {
      if (err.code === "DUPLICATE" && !lead) setDupe(err.message); else toast({ title: "Could not save lead", description: err.message, variant: "error" });
    } finally { setSaving(false); }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>{lead ? "Edit lead" : "Add lead"}</DialogTitle><DialogDescription>Priority and score are set automatically by the AI engine.</DialogDescription></DialogHeader>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Full name" error={errors.fullName} className="sm:col-span-2"><Input value={f.fullName} onChange={set("fullName")} /></Field>
          <Field label="Email" error={errors.email}><Input type="email" value={f.email} onChange={set("email")} /></Field>
          <Field label="Phone" error={errors.phone}><Input value={f.phone} onChange={set("phone")} placeholder="+91 98xxxxxxxx" /></Field>
          <Field label="Company"><Input value={f.company} onChange={set("company")} /></Field>
          <Field label="Source"><Select value={f.source} onChange={set("source")}>{LEAD_SOURCES.map((s) => <option key={s}>{s}</option>)}</Select></Field>
          <Field label="Pipeline stage"><Select value={f.stageId} onChange={set("stageId")}>{stages.map((s) => <option key={s.stageId} value={s.stageId}>{s.stageName}</option>)}</Select></Field>
          {can("Administrator", "Sales Manager") && (
            <Field label="Assigned to"><Select value={f.assignedTo} onChange={set("assignedTo")}>{users.map((u) => <option key={u.userId} value={u.userId}>{u.fullName}</option>)}</Select></Field>
          )}
        </div>
        {dupe && <p className="rounded-md bg-warning/10 p-3 text-sm text-warning">{dupe} Save anyway?</p>}
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          {dupe ? <Button onClick={() => save(true)} disabled={saving}>Save anyway</Button> : <Button onClick={() => save()} disabled={saving}>{saving ? "Saving…" : "Save lead"}</Button>}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
