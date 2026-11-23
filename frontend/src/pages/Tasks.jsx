import { useState } from "react";
import { Link } from "react-router-dom";
import { Check, X, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Field, Input, Select } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/toast";
import { PageHeader, Loading, ErrorState, EmptyState } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { leadsApi, remindersApi } from "@/services/api";
import { fmtDateTime, toLocalInput } from "@/lib/utils";

/** "My Tasks" (Module 6): all follow-up reminders grouped as Pending / Overdue / Done. */
export default function Tasks() {
  const { toast } = useToast();
  const { data, loading, error, reload } = useApi(() => remindersApi.list(), []);
  const leads = useApi(() => leadsApi.list(), []).data || [];
  const [open, setOpen] = useState(false);
  const [f, setF] = useState({ leadId: "", reminderTime: toLocalInput(new Date(Date.now() + 864e5)), message: "", priority: "Medium" });
  if (loading) return <Loading />;
  if (error) return <ErrorState error={error} />;

  const now = new Date();
  const pending = data.filter((r) => r.status === "Pending");
  const groups = { overdue: pending.filter((r) => new Date(r.reminderTime) < now), upcoming: pending.filter((r) => new Date(r.reminderTime) >= now), done: data.filter((r) => r.status !== "Pending") };
  const setStatus = async (id, s) => { await remindersApi.setStatus(id, s); reload(); };
  const add = async () => { if (!f.leadId || !f.message.trim()) return; await remindersApi.create(f); toast({ title: "Reminder set" }); setOpen(false); setF({ ...f, message: "" }); reload(); };

  const List = ({ items, empty }) => items.length === 0 ? <EmptyState title={empty} /> : (
    <div className="space-y-2">{items.map((r) => (
      <Card key={r.reminderId}><CardContent className="flex items-center gap-3 pt-4 sm:pt-5">
        <div className="min-w-0 flex-1"><p className="font-medium">{r.message}</p>
          <p className="text-xs text-muted-foreground"><Link to={`/leads/${r.leadId}`} className="hover:underline">{r.leadName}</Link> · {fmtDateTime(r.reminderTime)}</p></div>
        <Badge variant={r.priority === "High" ? "danger" : "secondary"}>{r.priority}</Badge>
        {r.status === "Pending" ? (<>
          <Button size="icon" variant="outline" aria-label="Mark complete" onClick={() => setStatus(r.reminderId, "Completed")}><Check /></Button>
          <Button size="icon" variant="ghost" aria-label="Cancel reminder" onClick={() => setStatus(r.reminderId, "Cancelled")}><X /></Button></>) : <Badge variant={r.status === "Completed" ? "success" : "secondary"}>{r.status}</Badge>}
      </CardContent></Card>))}</div>);

  return (
    <>
      <PageHeader title="My tasks" description="Follow-ups you have scheduled. Due items also appear as notifications."><Button size="sm" onClick={() => setOpen(true)}><Plus />New reminder</Button></PageHeader>
      <Tabs defaultValue={groups.overdue.length ? "overdue" : "upcoming"}>
        <TabsList><TabsTrigger value="overdue">Overdue ({groups.overdue.length})</TabsTrigger><TabsTrigger value="upcoming">Upcoming ({groups.upcoming.length})</TabsTrigger><TabsTrigger value="done">Done ({groups.done.length})</TabsTrigger></TabsList>
        <TabsContent value="overdue"><List items={groups.overdue} empty="Nothing overdue" /></TabsContent>
        <TabsContent value="upcoming"><List items={groups.upcoming} empty="No upcoming reminders" /></TabsContent>
        <TabsContent value="done"><List items={groups.done} empty="No completed reminders yet" /></TabsContent>
      </Tabs>
      <Dialog open={open} onOpenChange={setOpen}><DialogContent><DialogHeader><DialogTitle>New reminder</DialogTitle></DialogHeader>
        <div className="grid gap-3">
          <Field label="Lead"><Select value={f.leadId} onChange={(e) => setF({ ...f, leadId: e.target.value })}><option value="">Choose a lead</option>{leads.map((l) => <option key={l.leadId} value={l.leadId}>{l.fullName} ({l.company})</option>)}</Select></Field>
          <Field label="Remind me on"><Input type="datetime-local" value={f.reminderTime} onChange={(e) => setF({ ...f, reminderTime: e.target.value })} /></Field>
          <Field label="Priority"><Select value={f.priority} onChange={(e) => setF({ ...f, priority: e.target.value })}>{["High", "Medium", "Low"].map((p) => <option key={p}>{p}</option>)}</Select></Field>
          <Field label="Message"><Input value={f.message} onChange={(e) => setF({ ...f, message: e.target.value })} /></Field>
        </div><DialogFooter><Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button><Button onClick={add}>Set reminder</Button></DialogFooter></DialogContent></Dialog>
    </>
  );
}
