import { Loader2, Inbox } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export const PageHeader = ({ title, description, children }) => (
  <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
    <div><h1 className="text-xl font-semibold sm:text-2xl">{title}</h1>{description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}</div>
    {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
  </div>
);
export const Loading = ({ className }) => (
  <div className={cn("flex items-center justify-center py-16 text-muted-foreground", className)}><Loader2 className="size-5 animate-spin" aria-label="Loading" /></div>
);
export const EmptyState = ({ title, hint, action }) => (
  <div className="flex flex-col items-center gap-2 py-14 text-center">
    <Inbox className="size-8 text-muted-foreground/60" />
    <p className="font-medium">{title}</p>{hint && <p className="max-w-xs text-sm text-muted-foreground">{hint}</p>}{action}
  </div>
);
export const ErrorState = ({ error }) => <p className="py-10 text-center text-sm text-destructive">{error?.message || "Something went wrong. Try again."}</p>;
export const StatCard = ({ label, value, icon: Icon, hint }) => (
  <Card><CardContent className="flex items-start justify-between gap-3 pt-4 sm:pt-5">
    <div><p className="text-xs text-muted-foreground">{label}</p><p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>{hint && <p className="mt-0.5 text-xs text-muted-foreground">{hint}</p>}</div>
    {Icon && <span className="rounded-md bg-accent p-2 text-accent-foreground"><Icon className="size-4" /></span>}
  </CardContent></Card>
);

/* Colour-coded badges used everywhere so meaning stays consistent */
export const SentimentBadge = ({ value }) =>
  value ? <Badge variant={value === "Positive" ? "success" : value === "Negative" ? "danger" : "secondary"}>{value}</Badge> : null;
export const PriorityBadge = ({ value }) =>
  <Badge variant={value === "High" ? "danger" : value === "Medium" ? "warning" : "secondary"}>{value}</Badge>;
export const StageBadge = ({ name }) =>
  <Badge variant={name === "Won" ? "success" : name === "Lost" ? "danger" : "accent"}>{name}</Badge>;

/** Circular-ish AI score bar (0-100) */
export const ScoreBar = ({ score }) => (
  <div className="flex items-center gap-2" title={`AI score ${score}/100`}>
    <div className="h-1.5 w-16 overflow-hidden rounded-full bg-muted"><div className={cn("h-full rounded-full", score >= 65 ? "bg-destructive" : score >= 40 ? "bg-warning" : "bg-muted-foreground/50")} style={{ width: `${score}%` }} /></div>
    <span className="text-xs tabular-nums text-muted-foreground">{score}</span>
  </div>
);
