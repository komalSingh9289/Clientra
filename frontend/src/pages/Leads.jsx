import { useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Search, Upload, Download, FileText, Pencil, Trash2, Flag, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input, Select } from "@/components/ui/input";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { PageHeader, Loading, ErrorState, EmptyState, PriorityBadge, StageBadge, ScoreBar } from "@/components/shared/ui-bits";
import LeadFormDialog from "@/components/shared/LeadFormDialog";
import { useApi } from "@/hooks/useApi";
import { leadsApi, stagesApi } from "@/services/api";
import { LEAD_SOURCES } from "@/services/constants";
import { downloadFile, parseCSV, printTable, toCSV, timeAgo } from "@/lib/utils";

const COLS = [
  { key: "fullName", label: "Name" }, { key: "email", label: "Email" }, { key: "phone", label: "Phone" }, { key: "company", label: "Company" },
  { key: "source", label: "Source" }, { key: "stageName", label: "Stage" }, { key: "priorityLabel", label: "Priority" }, { key: "ownerName", label: "Assigned to" },
];
const PAGE = 10; // Client-side paging for the demo. With 10,000+ leads, send page/size to the API instead.

/** Lead list (UC-02): search, filters, add/edit/delete, CSV import + CSV/PDF export. */
export default function Leads() {
  const { toast } = useToast();
  const [filters, setFilters] = useState({ search: "", stageId: "", priority: "", source: "" });
  const [page, setPage] = useState(0);
  const [form, setForm] = useState({ open: false, lead: null });
  const [del, setDel] = useState(null);
  const [imp, setImp] = useState(null); // import result dialog
  const fileRef = useRef();
  const stages = useApi(() => stagesApi.list(), []).data || [];
  const { data, loading, error, reload } = useApi(() => leadsApi.list(filters), [filters]);
  const set = (k) => (e) => { setFilters({ ...filters, [k]: e.target.value }); setPage(0); };
  const rows = data || [];
  const paged = useMemo(() => rows.slice(page * PAGE, page * PAGE + PAGE), [rows, page]);

  const exportCSV = async () => { downloadFile("clientra-leads.csv", toCSV(COLS, rows)); await leadsApi.logExport("CSV"); toast({ title: "CSV exported" }); };
  const exportPDF = async () => { printTable("Clientra leads", COLS, rows); await leadsApi.logExport("PDF"); };
  const onFile = async (e) => {
    const file = e.target.files[0]; if (!file) return;
    const result = await leadsApi.bulkImport(parseCSV(await file.text()));
    setImp(result); reload(); e.target.value = "";
  };
  const confirmDelete = async () => { await leadsApi.remove(del.leadId); toast({ title: "Lead deleted" }); setDel(null); reload(); };

  return (
    <>
      <PageHeader title="Leads" description="Every prospect and customer in one searchable place.">
        <input ref={fileRef} type="file" accept=".csv" hidden onChange={onFile} />
        <Button variant="outline" size="sm" onClick={() => fileRef.current.click()}><Upload />Import CSV</Button>
        <Button variant="outline" size="sm" onClick={exportCSV} disabled={!rows.length}><Download />CSV</Button>
        <Button variant="outline" size="sm" onClick={exportPDF} disabled={!rows.length}><FileText />PDF</Button>
        <Button size="sm" onClick={() => setForm({ open: true, lead: null })}><Plus />Add lead</Button>
      </PageHeader>

      <div className="mb-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-[1fr_160px_140px_160px]">
        <div className="relative sm:col-span-2 lg:col-span-1"><Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <Input className="pl-9" placeholder="Search name, company, email or phone" value={filters.search} onChange={set("search")} aria-label="Search leads" /></div>
        <Select value={filters.stageId} onChange={set("stageId")} aria-label="Stage"><option value="">All stages</option>{stages.map((s) => <option key={s.stageId} value={s.stageId}>{s.stageName}</option>)}</Select>
        <Select value={filters.priority} onChange={set("priority")} aria-label="Priority"><option value="">All priorities</option>{["High", "Medium", "Low"].map((p) => <option key={p}>{p}</option>)}</Select>
        <Select value={filters.source} onChange={set("source")} aria-label="Source"><option value="">All sources</option>{LEAD_SOURCES.map((s) => <option key={s}>{s}</option>)}</Select>
      </div>

      <Card>
        {loading ? <Loading /> : error ? <ErrorState error={error} /> : rows.length === 0 ? (
          <EmptyState title="No leads match" hint="Clear the filters or add your first lead." action={<Button size="sm" onClick={() => setForm({ open: true, lead: null })}>Add lead</Button>} />
        ) : (<>
          <Table>
            <THead><TR><TH>Lead</TH><TH className="hidden md:table-cell">Stage</TH><TH>Priority</TH><TH className="hidden sm:table-cell">AI score</TH><TH className="hidden lg:table-cell">Owner</TH><TH className="hidden lg:table-cell">Last activity</TH><TH className="w-20" /></TR></THead>
            <TBody>{paged.map((l) => (
              <TR key={l.leadId}>
                <TD><Link to={`/leads/${l.leadId}`} className="flex items-center gap-1.5 font-medium hover:underline">{l.fullName}{l.flagged && <Flag className="size-3.5 text-destructive" aria-label="Flagged for review" />}</Link><p className="text-xs text-muted-foreground">{l.company}</p></TD>
                <TD className="hidden md:table-cell"><StageBadge name={l.stageName} /></TD>
                <TD><PriorityBadge value={l.priorityLabel} /></TD>
                <TD className="hidden sm:table-cell"><ScoreBar score={l.aiScore} /></TD>
                <TD className="hidden lg:table-cell text-muted-foreground">{l.ownerName}</TD>
                <TD className="hidden lg:table-cell text-muted-foreground">{l.lastActivityAt ? timeAgo(l.lastActivityAt) : "—"}</TD>
                <TD><div className="flex justify-end">
                  <Button variant="ghost" size="icon" aria-label={`Edit ${l.fullName}`} onClick={() => setForm({ open: true, lead: l })}><Pencil /></Button>
                  <Button variant="ghost" size="icon" aria-label={`Delete ${l.fullName}`} onClick={() => setDel(l)}><Trash2 /></Button></div></TD>
              </TR>))}</TBody>
          </Table>
          <div className="flex items-center justify-between border-t px-3 py-2 text-xs text-muted-foreground">
            <span>{page * PAGE + 1}–{Math.min(rows.length, page * PAGE + PAGE)} of {rows.length}</span>
            <div className="flex gap-1"><Button variant="outline" size="icon" className="size-8" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Previous page"><ChevronLeft /></Button>
              <Button variant="outline" size="icon" className="size-8" disabled={(page + 1) * PAGE >= rows.length} onClick={() => setPage(page + 1)} aria-label="Next page"><ChevronRight /></Button></div>
          </div>
        </>)}
      </Card>

      <LeadFormDialog open={form.open} lead={form.lead} onOpenChange={(o) => setForm({ ...form, open: o })} onSaved={reload} />

      <Dialog open={!!del} onOpenChange={() => setDel(null)}>
        <DialogContent><DialogHeader><DialogTitle>Delete {del?.fullName}?</DialogTitle><DialogDescription>This also removes their activities, deals and reminders. It is recorded in the audit log.</DialogDescription></DialogHeader>
          <DialogFooter><Button variant="outline" onClick={() => setDel(null)}>Cancel</Button><Button variant="destructive" onClick={confirmDelete}>Delete lead</Button></DialogFooter></DialogContent>
      </Dialog>

      <Dialog open={!!imp} onOpenChange={() => setImp(null)}>
        <DialogContent><DialogHeader><DialogTitle>Import finished</DialogTitle><DialogDescription>{imp?.imported} imported, {imp?.skipped.length} skipped. Expected columns: name, email, phone, company, source.</DialogDescription></DialogHeader>
          {imp?.skipped.length > 0 && <ul className="max-h-48 space-y-1 overflow-y-auto text-sm">{imp.skipped.map((s) => <li key={s.row} className="text-muted-foreground">Row {s.row}: {s.reason}</li>)}</ul>}
          <DialogFooter><Button onClick={() => setImp(null)}>Done</Button></DialogFooter></DialogContent>
      </Dialog>
    </>
  );
}
