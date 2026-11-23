import { useState } from "react";
import { Link } from "react-router-dom";
import { Search } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Input, Select } from "@/components/ui/input";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { PageHeader, Loading, ErrorState, EmptyState, SentimentBadge } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { activitiesApi } from "@/services/api";
import { ACTIVITY_TYPES } from "@/services/constants";
import { fmtDateTime } from "@/lib/utils";

/** Global interaction history (Module 4): every time-stamped touchpoint with its AI sentiment label. */
export default function Activities() {
  const { data, loading, error } = useApi(() => activitiesApi.list(), []);
  const [q, setQ] = useState(""); const [type, setType] = useState(""); const [sent, setSent] = useState("");
  if (loading) return <Loading />;
  if (error) return <ErrorState error={error} />;
  const rows = data.filter((a) => (!type || a.type === type) && (!sent || a.sentimentLabel === sent) && (!q || (a.description + a.leadName).toLowerCase().includes(q.toLowerCase())));
  const count = (l) => data.filter((a) => a.sentimentLabel === l).length;
  return (
    <>
      <PageHeader title="Activities" description="Every call, email, meeting and note across your leads.">
        <Badge variant="success">{count("Positive")} positive</Badge><Badge>{count("Neutral")} neutral</Badge><Badge variant="danger">{count("Negative")} negative</Badge>
      </PageHeader>
      <div className="mb-3 grid gap-2 sm:grid-cols-[1fr_150px_150px]">
        <div className="relative"><Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" /><Input className="pl-9" placeholder="Search notes or lead name" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search activities" /></div>
        <Select value={type} onChange={(e) => setType(e.target.value)} aria-label="Type"><option value="">All types</option>{ACTIVITY_TYPES.map((t) => <option key={t}>{t}</option>)}</Select>
        <Select value={sent} onChange={(e) => setSent(e.target.value)} aria-label="Sentiment"><option value="">All sentiment</option>{["Positive", "Neutral", "Negative"].map((t) => <option key={t}>{t}</option>)}</Select>
      </div>
      <Card>{rows.length === 0 ? <EmptyState title="No activities found" hint="Try different filters." /> : (
        <Table><THead><TR><TH>When</TH><TH>Lead</TH><TH>Type</TH><TH className="hidden md:table-cell">Note</TH><TH>Sentiment</TH></TR></THead>
          <TBody>{rows.map((a) => (
            <TR key={a.activityId}><TD className="whitespace-nowrap text-muted-foreground">{fmtDateTime(a.activityDate)}</TD>
              <TD><Link to={`/leads/${a.leadId}`} className="font-medium hover:underline">{a.leadName}</Link><p className="text-xs text-muted-foreground md:hidden">{a.description}</p></TD>
              <TD>{a.type}</TD><TD className="hidden max-w-md md:table-cell">{a.description}</TD><TD><SentimentBadge value={a.sentimentLabel} /></TD></TR>))}</TBody></Table>)}
      </Card>
    </>
  );
}
