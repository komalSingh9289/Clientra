import { Link } from "react-router-dom";
import { Users, UserPlus, IndianRupee, TrendingUp, AlertTriangle, CheckCircle2, Info, Clock } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { PageHeader, StatCard, Loading, ErrorState, PriorityBadge, ScoreBar, EmptyState } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { reportsApi } from "@/services/api";
import { useAuth } from "@/context/AuthContext";
import { compactINR, fmtINR, fmtDateTime } from "@/lib/utils";

/** Dashboard (UC-09): KPIs, charts, AI insights, top leads, overdue follow-ups. One call: GET /dashboard. */
export default function Dashboard() {
  const { user } = useAuth();
  const { data, loading, error } = useApi(() => reportsApi.dashboard(), []);
  if (loading) return <Loading />;
  if (error) return <ErrorState error={error} />;
  const { kpis, valueByStage, conversionTrend, topLeads, overdue, repPerformance, insights } = data;
  const Icon = { warning: AlertTriangle, success: CheckCircle2, info: Info };
  const tone = { warning: "text-warning", success: "text-success", info: "text-primary" };

  return (
    <>
      <PageHeader title={`Welcome back, ${user.fullName.split(" ")[0]}`} description="Here is where your pipeline stands today." />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard label="Active leads" value={kpis.activeLeads} icon={Users} />
        <StatCard label="New this week" value={kpis.newThisWeek} icon={UserPlus} />
        <StatCard label="Open pipeline value" value={compactINR(kpis.pipelineValue)} icon={IndianRupee} hint={fmtINR(kpis.pipelineValue)} />
        <StatCard label="Conversion rate" value={`${kpis.conversionRate}%`} icon={TrendingUp} />
      </div>

      <Card className="mt-4">
        <CardHeader><CardTitle>AI insights</CardTitle><CardDescription>Anomalies and trends spotted in your data</CardDescription></CardHeader>
        <CardContent>
          {insights.length === 0 ? <p className="text-sm text-muted-foreground">Nothing unusual right now.</p> : (
            <ul className="space-y-2">{insights.map((i, k) => { const I = Icon[i.type]; return (
              <li key={k} className="flex items-start gap-2 text-sm"><I className={`mt-0.5 size-4 shrink-0 ${tone[i.type]}`} />
                {i.leadId ? <Link to={`/leads/${i.leadId}`} className="hover:underline">{i.text}</Link> : i.text}</li>); })}</ul>)}
        </CardContent>
      </Card>

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Pipeline value by stage</CardTitle></CardHeader>
          <CardContent className="h-64">
            <ResponsiveContainer><BarChart data={valueByStage} margin={{ left: -10 }}>
              <CartesianGrid vertical={false} stroke="hsl(var(--border))" /><XAxis dataKey="stage" tickLine={false} axisLine={false} fontSize={12} />
              <YAxis tickFormatter={compactINR} tickLine={false} axisLine={false} fontSize={12} width={60} />
              <Tooltip formatter={(v) => fmtINR(v)} cursor={{ fill: "hsl(var(--muted))" }} />
              <Bar dataKey="value" name="Value" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} />
            </BarChart></ResponsiveContainer>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Conversion rate trend</CardTitle></CardHeader>
          <CardContent className="h-64">
            <ResponsiveContainer><LineChart data={conversionTrend} margin={{ left: -20 }}>
              <CartesianGrid vertical={false} stroke="hsl(var(--border))" /><XAxis dataKey="month" tickLine={false} axisLine={false} fontSize={12} />
              <YAxis unit="%" tickLine={false} axisLine={false} fontSize={12} /><Tooltip formatter={(v) => `${v}%`} />
              <Line dataKey="rate" name="Conversion" stroke="hsl(var(--primary))" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart></ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader><CardTitle>Top priority leads</CardTitle><CardDescription>Ranked by AI score</CardDescription></CardHeader>
          <CardContent className="px-0 sm:px-0">
            <Table><THead><TR><TH>Lead</TH><TH>Score</TH><TH>Priority</TH></TR></THead>
              <TBody>{topLeads.map((l) => (
                <TR key={l.leadId}><TD><Link to={`/leads/${l.leadId}`} className="font-medium hover:underline">{l.fullName}</Link><p className="text-xs text-muted-foreground">{l.company}</p></TD>
                  <TD><ScoreBar score={l.aiScore} /></TD><TD><PriorityBadge value={l.priorityLabel} /></TD></TR>))}</TBody></Table>
          </CardContent>
        </Card>
        <Card className="lg:col-span-2">
          <CardHeader><CardTitle>Overdue follow-ups</CardTitle></CardHeader>
          <CardContent>
            {overdue.length === 0 ? <EmptyState title="No overdue tasks" hint="You are on top of your follow-ups." /> : (
              <ul className="space-y-3">{overdue.map((r) => (
                <li key={r.reminderId} className="text-sm"><Link to={`/leads/${r.leadId}`} className="font-medium hover:underline">{r.leadName}</Link>
                  <p className="text-muted-foreground">{r.message}</p><p className="mt-0.5 flex items-center gap-1 text-xs text-destructive"><Clock className="size-3" />{fmtDateTime(r.reminderTime)}</p></li>))}</ul>)}
          </CardContent>
        </Card>
      </div>

      {repPerformance.length > 0 && (
        <Card className="mt-4">
          <CardHeader><CardTitle>Team performance</CardTitle></CardHeader>
          <CardContent className="px-0 sm:px-0">
            <Table><THead><TR><TH>Representative</TH><TH>Leads</TH><TH>Activities</TH><TH>Deals won</TH></TR></THead>
              <TBody>{repPerformance.map((r) => <TR key={r.name}><TD className="font-medium">{r.name}</TD><TD>{r.leads}</TD><TD>{r.activities}</TD><TD>{r.won}</TD></TR>)}</TBody></Table>
          </CardContent>
        </Card>
      )}
    </>
  );
}
