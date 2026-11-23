import { useState } from "react";
import { Sparkles, RefreshCw, Copy, Send, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input, Select, Textarea } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { Badge } from "@/components/ui/badge";
import { aiApi, activitiesApi } from "@/services/api";
import { TONES } from "@/services/constants";
import { Loading } from "@/components/shared/ui-bits";

/** Score breakdown (AI lead scoring, Section 3.5) */
export function ScoreCard({ lead, onRescore }) {
  const [factors, setFactors] = useState(null);
  const [busy, setBusy] = useState(false);
  const run = async () => { setBusy(true); const r = await aiApi.score(lead.leadId); setFactors(r.factors); setBusy(false); onRescore?.(); };
  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-2">
        <div><CardTitle>AI lead score</CardTitle><CardDescription>Conversion likelihood from behaviour signals</CardDescription></div>
        <Button size="sm" variant="outline" onClick={run} disabled={busy}><RefreshCw className={busy ? "animate-spin" : ""} />Re-score</Button>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-semibold tabular-nums">{lead.aiScore}<span className="text-sm font-normal text-muted-foreground"> / 100</span></p>
        {factors ? (
          <ul className="mt-3 space-y-2">{factors.map((f) => (
            <li key={f.label} className="text-xs"><div className="mb-1 flex justify-between"><span>{f.label}</span><span className="tabular-nums text-muted-foreground">{f.value}/{f.max}</span></div>
              <div className="h-1.5 rounded-full bg-muted"><div className="h-full rounded-full bg-primary" style={{ width: `${(f.value / f.max) * 100}%` }} /></div></li>))}
          </ul>
        ) : <p className="mt-2 text-xs text-muted-foreground">Select Re-score to see which signals drive this number.</p>}
      </CardContent>
    </Card>
  );
}

/** Interaction summary (Section 3.8) */
export function SummaryCard({ leadId }) {
  const [data, setData] = useState(null);
  const [busy, setBusy] = useState(false);
  const run = async () => { setBusy(true); setData(await aiApi.summary(leadId)); setBusy(false); };
  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-2">
        <div><CardTitle>Interaction summary</CardTitle><CardDescription>Key points and next actions for quick handoffs</CardDescription></div>
        <Button size="sm" variant="outline" onClick={run} disabled={busy}><Sparkles />{data ? "Refresh" : "Summarise"}</Button>
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        {busy && <Loading className="py-4" />}
        {!busy && !data && <p className="text-xs text-muted-foreground">Summarise this lead's history in one click.</p>}
        {!busy && data && (<>
          <p>{data.summary}</p>
          {!data.reliable && <p className="text-xs text-warning">Fewer than 3 interactions logged, so this summary may be thin.</p>}
          {data.keyPoints.length > 0 && <div><p className="mb-1 text-xs font-medium text-muted-foreground">Recent points</p><ul className="list-disc space-y-1 pl-4">{data.keyPoints.map((k, i) => <li key={i}>{k}</li>)}</ul></div>}
          <div><p className="mb-1 text-xs font-medium text-muted-foreground">Action items</p><ul className="list-disc space-y-1 pl-4">{data.actionItems.map((k, i) => <li key={i}>{k}</li>)}</ul></div>
          <Badge variant={data.sentimentTrend === "Declining" ? "danger" : data.sentimentTrend === "Improving" ? "success" : "secondary"}>Sentiment {data.sentimentTrend.toLowerCase()}</Badge>
        </>)}
      </CardContent>
    </Card>
  );
}

/**
 * AI draft message (UC-05, Section 7.5 flow): generate -> edit -> approve.
 * "Approve & log" saves the message as an Email activity, which triggers sentiment + re-scoring server-side.
 */
export function DraftComposer({ leadId, onLogged }) {
  const { toast } = useToast();
  const [tone, setTone] = useState("Friendly");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [busy, setBusy] = useState(false);
  const [best, setBest] = useState(null);

  const generate = async () => {
    setBusy(true);
    const [d, b] = await Promise.all([aiApi.draft(leadId, tone), aiApi.followUp(leadId)]);
    setSubject(d.subject); setBody(d.body); setBest(b); setBusy(false);
  };
  const approve = async () => {
    await activitiesApi.create({ leadId, type: "Email", description: `Sent: ${subject}. ${body.replace(/\n+/g, " ").slice(0, 160)}` });
    toast({ title: "Message approved and logged" }); setBody(""); setSubject(""); onLogged?.();
  };
  return (
    <Card>
      <CardHeader><CardTitle>AI draft message</CardTitle><CardDescription>Written from the lead's stage, history and mood. Review before sending.</CardDescription></CardHeader>
      <CardContent className="space-y-3">
        <div className="flex gap-2">
          <Select value={tone} onChange={(e) => setTone(e.target.value)} className="w-40" aria-label="Tone">{TONES.map((t) => <option key={t}>{t}</option>)}</Select>
          <Button onClick={generate} disabled={busy}><Sparkles />{busy ? "Drafting…" : body ? "Redraft" : "Generate draft"}</Button>
        </div>
        {body && (<>
          <Input value={subject} onChange={(e) => setSubject(e.target.value)} aria-label="Subject" />
          <Textarea value={body} onChange={(e) => setBody(e.target.value)} className="min-h-[190px]" aria-label="Message body" />
          {best && <p className="flex items-center gap-1.5 text-xs text-muted-foreground"><Clock className="size-3.5" />Best time to send: {best.day}, {best.window}. {best.reason}</p>}
          <div className="flex flex-wrap gap-2">
            <Button onClick={approve}><Send />Approve and log</Button>
            <Button variant="outline" onClick={() => { navigator.clipboard?.writeText(`${subject}\n\n${body}`); toast({ title: "Copied to clipboard" }); }}><Copy />Copy</Button>
          </div>
        </>)}
      </CardContent>
    </Card>
  );
}
