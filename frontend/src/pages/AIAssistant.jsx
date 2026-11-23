import { useState } from "react";
import { Link } from "react-router-dom";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, Textarea } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { PageHeader, Loading, PriorityBadge, ScoreBar, SentimentBadge } from "@/components/shared/ui-bits";
import { DraftComposer, SummaryCard } from "@/components/ai/AIPanels";
import { useApi } from "@/hooks/useApi";
import { aiApi, leadsApi } from "@/services/api";
import { fmtDateTime } from "@/lib/utils";

/** AI hub (Module 5): the four AI capabilities in one place, plus the request log (tbl_ai_logs). */
export default function AIAssistant() {
  const leads = useApi(() => leadsApi.list(), []);
  const logs = useApi(() => aiApi.logs(), []);
  const [leadId, setLeadId] = useState("");
  const picker = (
    <Select value={leadId} onChange={(e) => setLeadId(e.target.value)} className="mb-4 max-w-sm" aria-label="Choose lead">
      <option value="">Choose a lead</option>{(leads.data || []).map((l) => <option key={l.leadId} value={l.leadId}>{l.fullName} ({l.company})</option>)}
    </Select>);
  if (leads.loading) return <Loading />;
  const atRisk = leads.data.filter((l) => l.flagged);

  return (
    <>
      <PageHeader title="AI assistant" description="Draft messages, score leads, check sentiment and summarise history." />
      <Tabs defaultValue="draft">
        <TabsList><TabsTrigger value="draft">Draft message</TabsTrigger><TabsTrigger value="scoring">Lead scoring</TabsTrigger><TabsTrigger value="sentiment">Sentiment</TabsTrigger><TabsTrigger value="summary">Summaries</TabsTrigger><TabsTrigger value="log">AI log</TabsTrigger></TabsList>
        <TabsContent value="draft">{picker}{leadId ? <DraftComposer key={leadId} leadId={leadId} onLogged={leads.reload} /> : <Hint />}</TabsContent>
        <TabsContent value="summary">{picker}{leadId ? <SummaryCard key={leadId} leadId={leadId} /> : <Hint />}</TabsContent>
        <TabsContent value="scoring">
          <Card><CardContent className="px-0 pt-2 sm:px-0">
            <Table><THead><TR><TH>Lead</TH><TH>Score</TH><TH>Priority</TH><TH className="hidden sm:table-cell">Stage</TH></TR></THead>
              <TBody>{leads.data.map((l) => <TR key={l.leadId}><TD><Link to={`/leads/${l.leadId}`} className="font-medium hover:underline">{l.fullName}</Link><p className="text-xs text-muted-foreground">{l.company}</p></TD><TD><ScoreBar score={l.aiScore} /></TD><TD><PriorityBadge value={l.priorityLabel} /></TD><TD className="hidden sm:table-cell">{l.stageName}</TD></TR>)}</TBody></Table>
          </CardContent></Card>
        </TabsContent>
        <TabsContent value="sentiment" className="space-y-4"><SentimentTester />
          <Card><CardHeader><CardTitle>Leads needing attention</CardTitle><CardDescription>Latest interaction was negative</CardDescription></CardHeader><CardContent>
            {atRisk.length === 0 ? <p className="text-sm text-muted-foreground">No at-risk leads right now.</p> : <ul className="space-y-2">{atRisk.map((l) => <li key={l.leadId} className="flex items-center justify-between text-sm"><Link to={`/leads/${l.leadId}`} className="font-medium hover:underline">{l.fullName} <span className="font-normal text-muted-foreground">· {l.company}</span></Link><Badge variant="danger">At risk</Badge></li>)}</ul>}</CardContent></Card>
        </TabsContent>
        <TabsContent value="log">
          <Card><CardContent className="px-0 pt-2 sm:px-0">{logs.loading ? <Loading /> : logs.data.length === 0 ? <p className="p-5 text-sm text-muted-foreground">AI requests you make will be listed here.</p> :
            <Table><THead><TR><TH>Time</TH><TH>Request</TH><TH>Lead #</TH><TH className="hidden md:table-cell">Response</TH></TR></THead><TBody>{logs.data.map((g) => <TR key={g.logId}><TD className="whitespace-nowrap">{fmtDateTime(g.timestamp)}</TD><TD><Badge>{g.requestType}</Badge></TD><TD>{g.leadId}</TD><TD className="hidden max-w-sm truncate md:table-cell text-muted-foreground">{g.aiResponse}</TD></TR>)}</TBody></Table>}</CardContent></Card>
        </TabsContent>
      </Tabs>
    </>
  );
}
const Hint = () => <p className="text-sm text-muted-foreground">Choose a lead to begin.</p>;

/** Paste any customer message to see its sentiment (POST /ai/sentiment) */
function SentimentTester() {
  const [text, setText] = useState(""); const [res, setRes] = useState(null);
  return (
    <Card><CardHeader><CardTitle>Check the tone of a message</CardTitle></CardHeader><CardContent className="space-y-2">
      <Textarea value={text} onChange={(e) => setText(e.target.value)} placeholder="Paste a customer email or note" />
      <div className="flex items-center gap-3"><Button onClick={async () => setRes(await aiApi.sentiment(text))} disabled={!text.trim()}><Sparkles />Analyse</Button>
        {res && <span className="flex items-center gap-2 text-sm"><SentimentBadge value={res.label} /><span className="text-muted-foreground">{Math.round(res.confidence * 100)}% confidence</span></span>}</div>
    </CardContent></Card>);
}
