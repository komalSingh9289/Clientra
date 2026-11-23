import { useState } from "react";
import { Download, FileText } from "lucide-react";
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input, Select, Field } from "@/components/ui/input";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { PageHeader, StatCard, Loading, EmptyState, PriorityBadge } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { leadsApi, reportsApi, stagesApi, usersApi } from "@/services/api";
import { LEAD_SOURCES } from "@/services/constants";
import { compactINR, downloadFile, fmtDate, fmtINR, printTable, toCSV } from "@/lib/utils";
import { Users, IndianRupee, Target } from "lucide-react";

const COLS = [{ key: "fullName", label: "Lead" }, { key: "company", label: "Company" }, { key: "source", label: "Source" }, { key: "stageName", label: "Stage" }, { key: "priorityLabel", label: "Priority" }, { key: "ownerName", label: "Representative" }, { key: "openValue", label: "Open value (INR)" }, { key: "createdOn", label: "Created" }];
const COLORS = ["hsl(233 55% 42%)", "hsl(233 40% 62%)", "hsl(180 30% 40%)", "hsl(33 70% 52%)", "hsl(152 40% 40%)", "hsl(0 0% 62%)"];

/** Reports (UC-10, UC-11): filter by date range, stage, source, representative; export CSV or PDF. */
export default function Reports() {
  const [f, setF] = useState({ from: "", to: "", stageId: "", source: "", assignedTo: "" });
  const stages = useApi(() => stagesApi.list(), []).data || [];
  const users = useApi(() => usersApi.assignable(), []).data || [];
  const { data, loading } = useApi(() => reportsApi.report(f), [f]);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const rows = (data?.rows || []).map((r) => ({ ...r, createdOn: fmtDate(r.createdAt) }));

  const csv = async () => { downloadFile("clientra-report.csv", toCSV(COLS, rows)); await leadsApi.logExport("CSV report"); };
  const pdf = async () => { printTable("Clientra report", COLS, rows); await leadsApi.logExport("PDF report"); };

  return (
    <>
      <PageHeader title="Reports" description="Slice your pipeline by date, stage, source or representative.">
        <Button variant="outline" size="sm" onClick={csv} disabled={!rows.length}><Download />CSV</Button><Button variant="outline" size="sm" onClick={pdf} disabled={!rows.length}><FileText />PDF</Button>
      </PageHeader>
      <Card className="mb-4"><CardContent className="grid gap-3 pt-4 sm:grid-cols-2 lg:grid-cols-5 sm:pt-5">
        <Field label="From"><Input type="date" value={f.from} onChange={set("from")} /></Field>
        <Field label="To"><Input type="date" value={f.to} onChange={set("to")} /></Field>
        <Field label="Stage"><Select value={f.stageId} onChange={set("stageId")}><option value="">All</option>{stages.map((s) => <option key={s.stageId} value={s.stageId}>{s.stageName}</option>)}</Select></Field>
        <Field label="Source"><Select value={f.source} onChange={set("source")}><option value="">All</option>{LEAD_SOURCES.map((s) => <option key={s}>{s}</option>)}</Select></Field>
        <Field label="Representative"><Select value={f.assignedTo} onChange={set("assignedTo")}><option value="">All</option>{users.map((u) => <option key={u.userId} value={u.userId}>{u.fullName}</option>)}</Select></Field>
      </CardContent></Card>

      {loading && !data ? <Loading /> : rows.length === 0 ? <Card><EmptyState title="No leads match these filters" hint="Widen the date range or clear a filter." /></Card> : (<>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
          <StatCard label="Leads" value={rows.length} icon={Users} /><StatCard label="Open value" value={compactINR(data.totalValue)} icon={IndianRupee} hint={fmtINR(data.totalValue)} />
          <StatCard label="High priority" value={rows.filter((r) => r.priorityLabel === "High").length} icon={Target} /></div>
        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <Card><CardHeader><CardTitle>Leads by stage</CardTitle></CardHeader><CardContent className="h-60"><ResponsiveContainer><BarChart data={data.byStage} margin={{ left: -25 }}><CartesianGrid vertical={false} stroke="hsl(var(--border))" /><XAxis dataKey="name" fontSize={11} tickLine={false} axisLine={false} /><YAxis allowDecimals={false} fontSize={12} tickLine={false} axisLine={false} /><Tooltip /><Bar dataKey="value" name="Leads" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></CardContent></Card>
          <Card><CardHeader><CardTitle>Leads by source</CardTitle></CardHeader><CardContent className="h-60"><ResponsiveContainer><PieChart><Pie data={data.bySource} dataKey="value" nameKey="name" innerRadius={45} outerRadius={80} label={({ name }) => name} labelLine={false} fontSize={11}>{data.bySource.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer></CardContent></Card>
        </div>
        <Card className="mt-4"><CardContent className="px-0 pt-2 sm:px-0"><Table><THead><TR><TH>Lead</TH><TH>Source</TH><TH>Stage</TH><TH>Priority</TH><TH className="hidden sm:table-cell">Representative</TH><TH className="text-right">Open value</TH></TR></THead>
          <TBody>{rows.map((r) => <TR key={r.leadId}><TD className="font-medium">{r.fullName}<p className="text-xs font-normal text-muted-foreground">{r.company}</p></TD><TD>{r.source}</TD><TD>{r.stageName}</TD><TD><PriorityBadge value={r.priorityLabel} /></TD><TD className="hidden sm:table-cell">{r.ownerName}</TD><TD className="text-right tabular-nums">{fmtINR(r.openValue)}</TD></TR>)}</TBody></Table></CardContent></Card>
      </>)}
    </>
  );
}
