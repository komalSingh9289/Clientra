import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Mail, Phone, Building2, Flag, Pencil, Plus, Phone as PhoneIcon, Users as Meeting, StickyNote } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Field, Input, Select, Textarea } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/components/ui/toast";
import { Loading, ErrorState, EmptyState, PriorityBadge, SentimentBadge } from "@/components/shared/ui-bits";
import LeadFormDialog from "@/components/shared/LeadFormDialog";
import { ScoreCard, SummaryCard, DraftComposer } from "@/components/ai/AIPanels";
import { useApi } from "@/hooks/useApi";
import { activitiesApi, dealsApi, leadsApi, remindersApi, stagesApi } from "@/services/api";
import { ACTIVITY_TYPES, DEAL_STATUS } from "@/services/constants";
import { fmtDate, fmtDateTime, fmtINR, toLocalInput } from "@/lib/utils";

const ICON = { Call: PhoneIcon, Email: Mail, Meeting, Note: StickyNote };

/**
 * Lead 360 view. Follows the process flow in Section 7.5:
 * view AI summary + history -> log interaction -> sentiment runs (negative = flagged)
 * -> request AI draft -> re-score -> set next reminder / move stage.
 */
export default function LeadDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const { toast } = useToast();
  const lead = useApi(() => leadsApi.get(id), [id]);
  const acts = useApi(() => activitiesApi.list({ leadId: id }), [id]);
  const deals = useApi(() => dealsApi.byLead(id), [id]);
  const stages = useApi(() => stagesApi.list(), []).data || [];
  const [edit, setEdit] = useState(false);
  const refresh = () => { lead.reload(); acts.reload(); deals.reload(); };

  if (lead.loading && !lead.data) return <Loading />;
  if (lead.error) return <ErrorState error={lead.error} />;
  const l = lead.data;

  const changeStage = async (e) => { await leadsApi.updateStage(l.leadId, e.target.value); toast({ title: "Stage updated" }); refresh(); };

  return (
    <>
      <Link to="/leads" className="mb-3 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"><ArrowLeft className="size-4" />All leads</Link>
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2"><h1 className="text-xl font-semibold sm:text-2xl">{l.fullName}</h1><PriorityBadge value={l.priorityLabel} />
            {l.flagged && <Badge variant="danger"><Flag className="size-3" />Priority review</Badge>}</div>
          <p className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
            <span className="flex items-center gap-1"><Building2 className="size-3.5" />{l.company}</span>
            <span className="flex items-center gap-1"><Mail className="size-3.5" />{l.email}</span>
            <span className="flex items-center gap-1"><Phone className="size-3.5" />{l.phone}</span></p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={l.stageId} onChange={changeStage} className="w-40" aria-label="Pipeline stage">{stages.map((s) => <option key={s.stageId} value={s.stageId}>{s.stageName}</option>)}</Select>
          <Button variant="outline" onClick={() => setEdit(true)}><Pencil />Edit</Button>
        </div>
      </div>
      {l.flagged && <p className="mb-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive">The latest interaction showed negative sentiment. Address the customer's concerns before the next sales push.</p>}

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:order-2"><ScoreCard lead={l} onRescore={lead.reload} />
          <Card><CardContent className="space-y-1.5 pt-4 text-sm sm:pt-5">
            <Row k="Source" v={l.source} /><Row k="Assigned to" v={l.ownerName} /><Row k="Created" v={fmtDate(l.createdAt)} /><Row k="Days in stage" v={l.daysInStage} /><Row k="Open deal value" v={fmtINR(l.openValue)} /></CardContent></Card>
        </div>
        <div className="lg:order-1 lg:col-span-2">
          <Tabs defaultValue="timeline">
            <TabsList><TabsTrigger value="timeline">Timeline</TabsTrigger><TabsTrigger value="ai">AI tools</TabsTrigger><TabsTrigger value="deals">Deals</TabsTrigger><TabsTrigger value="reminders">Reminders</TabsTrigger></TabsList>
            <TabsContent value="timeline" className="space-y-4"><LogInteraction leadId={l.leadId} onDone={refresh} /><Timeline items={acts.data} loading={acts.loading} /></TabsContent>
            <TabsContent value="ai" className="space-y-4"><SummaryCard leadId={l.leadId} /><DraftComposer leadId={l.leadId} onLogged={refresh} /></TabsContent>
            <TabsContent value="deals"><Deals leadId={l.leadId} items={deals.data} onChange={refresh} /></TabsContent>
            <TabsContent value="reminders"><LeadReminders leadId={l.leadId} /></TabsContent>
          </Tabs>
        </div>
      </div>
      <LeadFormDialog open={edit} onOpenChange={setEdit} lead={l} onSaved={refresh} />
    </>
  );
}

const Row = ({ k, v }) => <div className="flex justify-between gap-3"><span className="text-muted-foreground">{k}</span><span className="text-right font-medium">{v}</span></div>;

/** Log a call/email/meeting/note (UC-04). The API runs sentiment analysis on the text automatically. */
function LogInteraction({ leadId, onDone }) {
  const { toast } = useToast();
  const [type, setType] = useState("Call"); const [text, setText] = useState(""); const [busy, setBusy] = useState(false);
  const submit = async () => {
    if (!text.trim()) return;
    setBusy(true);
    const r = await activitiesApi.create({ leadId, type, description: text });
    toast({ title: "Interaction logged", description: `Sentiment: ${r.sentiment.label}${r.flagged ? ". Lead flagged for priority review." : ""}`, variant: r.flagged ? "info" : "default" });
    setText(""); setBusy(false); onDone();
  };
  return (
    <Card><CardHeader><CardTitle>Log interaction</CardTitle></CardHeader><CardContent className="space-y-2">
      <div className="flex gap-2"><Select value={type} onChange={(e) => setType(e.target.value)} className="w-36" aria-label="Type">{ACTIVITY_TYPES.map((t) => <option key={t}>{t}</option>)}</Select></div>
      <Textarea placeholder="What happened? e.g. Called to discuss pricing, they loved the demo." value={text} onChange={(e) => setText(e.target.value)} />
      <Button onClick={submit} disabled={busy || !text.trim()}>{busy ? "Analysing…" : "Save interaction"}</Button>
    </CardContent></Card>
  );
}

function Timeline({ items, loading }) {
  if (loading) return <Loading className="py-6" />;
  if (!items?.length) return <EmptyState title="No interactions yet" hint="Log your first call, email or meeting above." />;
  return (
    <ol className="space-y-3 border-l pl-4">{items.map((a) => { const I = ICON[a.type] || StickyNote; return (
      <li key={a.activityId} className="relative">
        <span className="absolute -left-[25px] top-0.5 grid size-5 place-items-center rounded-full border bg-card"><I className="size-3 text-muted-foreground" /></span>
        <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground"><span className="font-medium text-foreground">{a.type}</span><span>{fmtDateTime(a.activityDate)}</span><span>by {a.userName}</span><SentimentBadge value={a.sentimentLabel} /></div>
        <p className="mt-1 text-sm">{a.description}</p></li>); })}</ol>
  );
}

function Deals({ leadId, items = [], onChange }) {
  const [amount, setAmount] = useState(""); const [closing, setClosing] = useState("");
  const add = async () => { if (!amount) return; await dealsApi.create({ leadId, amount, closingDate: closing, notes: "" }); setAmount(""); setClosing(""); onChange(); };
  return (
    <div className="space-y-4">
      <Card><CardContent className="grid gap-2 pt-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end sm:pt-5">
        <Field label="Deal amount (₹)"><Input type="number" min="0" value={amount} onChange={(e) => setAmount(e.target.value)} /></Field>
        <Field label="Expected closing date"><Input type="date" value={closing} onChange={(e) => setClosing(e.target.value)} /></Field>
        <Button onClick={add}><Plus />Add deal</Button></CardContent></Card>
      {items.length === 0 ? <EmptyState title="No deals yet" hint="Add a deal to include this lead in pipeline value." /> : items.map((d) => (
        <Card key={d.dealId}><CardContent className="flex items-center justify-between gap-3 pt-4 sm:pt-5">
          <div><p className="font-medium">{fmtINR(d.amount)}</p><p className="text-xs text-muted-foreground">Closing {fmtDate(d.closingDate)}</p></div>
          <Select value={d.status} className="w-28" aria-label="Deal status" onChange={async (e) => { await dealsApi.setStatus(d.dealId, e.target.value); onChange(); }}>{DEAL_STATUS.map((s) => <option key={s}>{s}</option>)}</Select></CardContent></Card>))}
    </div>
  );
}

/** Set follow-up reminder for this lead (UC-08). Full task list lives on /tasks. */
function LeadReminders({ leadId }) {
  const { toast } = useToast();
  const { data, reload } = useApi(() => remindersApi.list(), []);
  const [f, setF] = useState({ reminderTime: toLocalInput(new Date(Date.now() + 864e5)), message: "", priority: "Medium" });
  const mine = (data || []).filter((r) => r.leadId === leadId);
  const add = async () => { if (!f.message.trim()) return; await remindersApi.create({ ...f, leadId }); toast({ title: "Reminder set" }); setF({ ...f, message: "" }); reload(); };
  return (
    <div className="space-y-4">
      <Card><CardContent className="grid gap-2 pt-4 sm:grid-cols-2 sm:pt-5">
        <Field label="Remind me on"><Input type="datetime-local" value={f.reminderTime} onChange={(e) => setF({ ...f, reminderTime: e.target.value })} /></Field>
        <Field label="Priority"><Select value={f.priority} onChange={(e) => setF({ ...f, priority: e.target.value })}>{["High", "Medium", "Low"].map((p) => <option key={p}>{p}</option>)}</Select></Field>
        <Field label="Message" className="sm:col-span-2"><Input value={f.message} onChange={(e) => setF({ ...f, message: e.target.value })} placeholder="e.g. Send revised quote" /></Field>
        <Button className="sm:col-span-2 sm:w-fit" onClick={add}><Plus />Set reminder</Button></CardContent></Card>
      {mine.length === 0 ? <EmptyState title="No reminders for this lead" /> : mine.map((r) => (
        <Card key={r.reminderId}><CardContent className="flex items-center justify-between gap-3 pt-4 text-sm sm:pt-5"><div><p className="font-medium">{r.message}</p><p className="text-xs text-muted-foreground">{fmtDateTime(r.reminderTime)}</p></div><Badge variant={r.status === "Completed" ? "success" : r.status === "Cancelled" ? "secondary" : "warning"}>{r.status}</Badge></CardContent></Card>))}
    </div>
  );
}
