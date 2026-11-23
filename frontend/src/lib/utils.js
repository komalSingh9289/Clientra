import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/** Merge Tailwind class names safely (shadcn standard helper). */
export const cn = (...inputs) => twMerge(clsx(inputs));

/* ---------- Formatting helpers ---------- */
export const fmtDate = (d) => (d ? new Date(d).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "—");
export const fmtDateTime = (d) =>
  d ? new Date(d).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" }) : "—";
export const fmtINR = (n) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(n || 0);
/** ₹1.2L / ₹3.4Cr style, used where space is tight (cards, chart axes) */
export const compactINR = (n = 0) =>
  n >= 1e7 ? `₹${(n / 1e7).toFixed(1)}Cr` : n >= 1e5 ? `₹${(n / 1e5).toFixed(1)}L` : n >= 1e3 ? `₹${(n / 1e3).toFixed(0)}K` : `₹${n}`;
export const initials = (name = "") => name.split(" ").map((p) => p[0]).slice(0, 2).join("").toUpperCase();
export const daysBetween = (a, b = new Date()) => Math.floor((new Date(b) - new Date(a)) / 864e5);
export const timeAgo = (d) => {
  const mins = Math.floor((Date.now() - new Date(d)) / 6e4);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  if (mins < 1440) return `${Math.floor(mins / 60)}h ago`;
  return `${Math.floor(mins / 1440)}d ago`;
};
/** Value for <input type="datetime-local"> */
export const toLocalInput = (d = new Date()) => {
  const x = new Date(d); x.setMinutes(x.getMinutes() - x.getTimezoneOffset());
  return x.toISOString().slice(0, 16);
};

/* ---------- Validation ---------- */
export const isEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v || "");

/* ---------- CSV / PDF export helpers (client-side; swap for server exports if needed) ---------- */
export function downloadFile(filename, content, mime = "text/csv") {
  const url = URL.createObjectURL(new Blob([content], { type: mime }));
  const a = Object.assign(document.createElement("a"), { href: url, download: filename });
  a.click();
  URL.revokeObjectURL(url);
}
/** columns: [{ key, label }]; rows: array of objects */
export function toCSV(columns, rows) {
  const esc = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
  return [columns.map((c) => esc(c.label)).join(","), ...rows.map((r) => columns.map((c) => esc(r[c.key])).join(","))].join("\n");
}
/** Minimal CSV parser (handles quoted values). Returns array of objects keyed by lower-cased header. */
export function parseCSV(text) {
  const rows = []; let row = [], cell = "", q = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (q) { if (c === '"' && text[i + 1] === '"') { cell += '"'; i++; } else if (c === '"') q = false; else cell += c; }
    else if (c === '"') q = true;
    else if (c === ",") { row.push(cell); cell = ""; }
    else if (c === "\n" || c === "\r") { if (c === "\r" && text[i + 1] === "\n") i++; row.push(cell); rows.push(row); row = []; cell = ""; }
    else cell += c;
  }
  if (cell || row.length) { row.push(cell); rows.push(row); }
  const [head, ...body] = rows.filter((r) => r.some((x) => x.trim()));
  if (!head) return [];
  const keys = head.map((h) => h.trim().toLowerCase().replace(/[\s_]+/g, ""));
  return body.map((r) => Object.fromEntries(keys.map((k, i) => [k, (r[i] || "").trim()])));
}
/** "Export PDF" = open a print-ready table; user picks "Save as PDF". Replace with GET /reports/export?format=pdf later. */
export function printTable(title, columns, rows) {
  const w = window.open("", "_blank");
  if (!w) return;
  const th = columns.map((c) => `<th>${c.label}</th>`).join("");
  const tr = rows.map((r) => `<tr>${columns.map((c) => `<td>${r[c.key] ?? ""}</td>`).join("")}</tr>`).join("");
  w.document.write(`<html><head><title>${title}</title><style>body{font-family:system-ui;padding:24px}table{border-collapse:collapse;width:100%;font-size:12px}th,td{border:1px solid #ddd;padding:6px 8px;text-align:left}th{background:#f4f4f6}</style></head><body><h2>${title}</h2><table><thead><tr>${th}</tr></thead><tbody>${tr}</tbody></table></body></html>`);
  w.document.close(); w.focus(); w.print();
}
