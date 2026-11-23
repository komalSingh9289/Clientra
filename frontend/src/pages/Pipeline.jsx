import { useState } from "react";
import { Link } from "react-router-dom";
import { Flag } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/input";
import { PageHeader, Loading, ErrorState, PriorityBadge } from "@/components/shared/ui-bits";
import { useToast } from "@/components/ui/toast";
import { useApi } from "@/hooks/useApi";
import { leadsApi, stagesApi } from "@/services/api";
import { cn, compactINR } from "@/lib/utils";

/**
 * Kanban pipeline (UC-03, Section 3.3). Drag a card between columns (desktop) or use the
 * "Move to" dropdown (touch devices). Each column header shows the aggregate metrics from Module 3:
 * lead count, pipeline value and average days-in-stage (bottleneck indicator).
 * Stages come from the API, so Admin/Manager changes in Settings appear here automatically.
 */
export default function Pipeline() {
  const { toast } = useToast();
  const stages = useApi(() => stagesApi.list(), []);
  const leads = useApi(() => leadsApi.list(), []);
  const [dragId, setDragId] = useState(null);
  const [over, setOver] = useState(null);
  if (stages.loading || leads.loading) return <Loading />;
  if (stages.error || leads.error) return <ErrorState error={stages.error || leads.error} />;

  const move = async (leadId, stageId) => {
    await leadsApi.updateStage(leadId, stageId); toast({ title: "Lead moved" }); leads.reload();
  };
  const drop = (stageId) => { if (dragId) move(dragId, stageId); setDragId(null); setOver(null); };

  return (
    <>
      <PageHeader title="Sales pipeline" description="Drag leads between stages as deals progress." />
      <div className="-mx-4 flex gap-3 overflow-x-auto px-4 pb-4 lg:mx-0 lg:px-0">
        {stages.data.map((s) => {
          const items = leads.data.filter((l) => l.stageId === s.stageId);
          const value = items.reduce((a, l) => a + l.openValue, 0);
          const avg = items.length ? Math.round(items.reduce((a, l) => a + l.daysInStage, 0) / items.length) : 0;
          return (
            <section key={s.stageId} onDragOver={(e) => { e.preventDefault(); setOver(s.stageId); }} onDragLeave={() => setOver(null)} onDrop={() => drop(s.stageId)}
              className={cn("w-72 shrink-0 rounded-lg border bg-muted/40 p-2 transition-colors", over === s.stageId && "border-primary bg-accent")} aria-label={`${s.stageName} stage`}>
              <header className="px-2 pb-2 pt-1">
                <div className="flex items-center justify-between"><h2 className="text-sm font-semibold">{s.stageName}</h2><span className="text-xs text-muted-foreground">{items.length}</span></div>
                <p className="text-xs text-muted-foreground">{compactINR(value)} · avg {avg}d in stage</p>
              </header>
              <div className="space-y-2">
                {items.map((l) => (
                  <Card key={l.leadId} draggable onDragStart={() => setDragId(l.leadId)} onDragEnd={() => { setDragId(null); setOver(null); }} className={cn("cursor-grab p-3 active:cursor-grabbing", dragId === l.leadId && "opacity-50")}>
                    <div className="flex items-start justify-between gap-2">
                      <Link to={`/leads/${l.leadId}`} className="text-sm font-medium hover:underline">{l.fullName}</Link>{l.flagged && <Flag className="size-3.5 shrink-0 text-destructive" aria-label="Flagged" />}
                    </div>
                    <p className="text-xs text-muted-foreground">{l.company}</p>
                    <div className="mt-2 flex items-center justify-between"><PriorityBadge value={l.priorityLabel} /><span className="text-xs tabular-nums text-muted-foreground">{l.openValue ? compactINR(l.openValue) : "No deal"}</span></div>
                    <Select value={l.stageId} onChange={(e) => move(l.leadId, e.target.value)} className="mt-2 h-8 text-xs" aria-label={`Move ${l.fullName} to stage`}>{stages.data.map((x) => <option key={x.stageId} value={x.stageId}>Move to: {x.stageName}</option>)}</Select>
                  </Card>))}
                {items.length === 0 && <p className="px-2 py-6 text-center text-xs text-muted-foreground">Drop a lead here</p>}
              </div>
            </section>);
        })}
      </div>
    </>
  );
}
