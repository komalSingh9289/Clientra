import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Select } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { PageHeader, Loading, ErrorState, EmptyState } from "@/components/shared/ui-bits";
import { useApi } from "@/hooks/useApi";
import { auditApi } from "@/services/api";
import { fmtDateTime } from "@/lib/utils";

/** UC-14 Audit log (Admin only). Read-only: entries are written by the server and cannot be edited (Section 11.6). */
export default function AuditLog() {
  const { data, loading, error } = useApi(() => auditApi.list(), []);
  const [action, setAction] = useState("");
  if (loading) return <Loading />;
  if (error) return <ErrorState error={error} />;
  const actions = [...new Set(data.map((a) => a.action))];
  const rows = data.filter((a) => !action || a.action === action);
  return (
    <>
      <PageHeader title="Audit log" description="Who did what, and when, for every sensitive operation."><Select value={action} onChange={(e) => setAction(e.target.value)} className="w-44" aria-label="Filter by action"><option value="">All actions</option>{actions.map((a) => <option key={a}>{a}</option>)}</Select></PageHeader>
      <Card>{rows.length === 0 ? <EmptyState title="No entries" /> : (
        <Table><THead><TR><TH>Time</TH><TH>User</TH><TH>Action</TH><TH className="hidden sm:table-cell">Record</TH><TH>Detail</TH></TR></THead>
          <TBody>{rows.map((a) => <TR key={a.id}><TD className="whitespace-nowrap text-muted-foreground">{fmtDateTime(a.timestamp)}</TD><TD>{a.userName}</TD><TD><Badge>{a.action}</Badge></TD><TD className="hidden sm:table-cell">{a.entity}</TD><TD>{a.detail}</TD></TR>)}</TBody></Table>)}</Card>
    </>
  );
}
