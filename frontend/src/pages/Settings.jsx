import { useState } from "react";
import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { PageHeader, Loading } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { settingsApi, stagesApi } from "@/services/api";
import { useAuth } from "@/context/AuthContext";

/** UC-12 Configure pipeline stages (Admin + Sales Manager) and email notification rules (Module 8, Admin only). */
export default function Settings() {
  const { can } = useAuth();
  const { toast } = useToast();
  const stages = useApi(() => stagesApi.list(), []);
  const rules = useApi(() => settingsApi.getRules(), []);
  const [name, setName] = useState("");
  const run = async (fn, ok) => { try { await fn(); ok && toast({ title: ok }); stages.reload(); } catch (e) { toast({ title: "Could not complete that", description: e.message, variant: "error" }); } };
  if (stages.loading || rules.loading) return <Loading />;

  const move = (i, d) => { const ids = stages.data.map((s) => s.stageId); [ids[i], ids[i + d]] = [ids[i + d], ids[i]]; run(() => stagesApi.reorder(ids)); };
  const RULES = [["emailOnDue", "Email me when a reminder is due"], ["emailOnOverdue", "Email me when a reminder is overdue"], ["emailOnNegative", "Alert the owner when sentiment turns negative"], ["weeklyDigest", "Send a weekly pipeline digest to managers"]];

  return (
    <>
      <PageHeader title="Pipeline and rules" description="Control how leads move through your pipeline and when people are notified." />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card><CardHeader><CardTitle>Pipeline stages</CardTitle><CardDescription>Order here is the order on the pipeline board.</CardDescription></CardHeader>
          <CardContent className="space-y-2">
            {stages.data.map((s, i) => (
              <div key={s.stageId} className="flex items-center gap-2">
                <Input defaultValue={s.stageName} aria-label={`Stage ${i + 1} name`} onBlur={(e) => e.target.value.trim() && e.target.value !== s.stageName && run(() => stagesApi.rename(s.stageId, e.target.value.trim()), "Stage renamed")} />
                <Button size="icon" variant="ghost" disabled={i === 0} onClick={() => move(i, -1)} aria-label="Move up"><ArrowUp /></Button>
                <Button size="icon" variant="ghost" disabled={i === stages.data.length - 1} onClick={() => move(i, 1)} aria-label="Move down"><ArrowDown /></Button>
                <Button size="icon" variant="ghost" onClick={() => run(() => stagesApi.remove(s.stageId), "Stage deleted")} aria-label={`Delete ${s.stageName}`}><Trash2 /></Button>
              </div>))}
            <div className="flex gap-2 pt-2"><Input placeholder="New stage name" value={name} onChange={(e) => setName(e.target.value)} />
              <Button onClick={() => name.trim() && run(async () => { await stagesApi.create(name.trim()); setName(""); }, "Stage added")}><Plus />Add</Button></div>
          </CardContent></Card>

        {can("Administrator") && (
          <Card><CardHeader><CardTitle>Email notification rules</CardTitle><CardDescription>Sent through the configured SMTP server. Real-time alerts in the bell work regardless.</CardDescription></CardHeader>
            <CardContent className="space-y-3">
              <RuleForm initial={rules.data} items={RULES} onSave={async (r) => { await settingsApi.saveRules(r); toast({ title: "Rules saved" }); }} />
            </CardContent></Card>)}
      </div>
    </>
  );
}

function RuleForm({ initial, items, onSave }) {
  const [r, setR] = useState(initial);
  return (<>
    {items.map(([k, label]) => (
      <label key={k} className="flex cursor-pointer items-center justify-between gap-3 text-sm"><span>{label}</span>
        <input type="checkbox" className="size-4 accent-[hsl(var(--primary))]" checked={r[k]} onChange={(e) => setR({ ...r, [k]: e.target.checked })} /></label>))}
    <Button onClick={() => onSave(r)}>Save rules</Button></>);
}
