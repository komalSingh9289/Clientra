import { cn } from "@/lib/utils";

/** Wrapper provides horizontal scroll so wide tables never break the page on mobile */
export const Table = ({ className, ...p }) => (
  <div className="w-full overflow-x-auto"><table className={cn("w-full caption-bottom text-sm", className)} {...p} /></div>
);
export const THead = ({ className, ...p }) => <thead className={cn("border-b bg-muted/50 text-left text-xs text-muted-foreground", className)} {...p} />;
export const TBody = ({ className, ...p }) => <tbody className={cn("[&_tr:last-child]:border-0", className)} {...p} />;
export const TR = ({ className, ...p }) => <tr className={cn("border-b transition-colors hover:bg-muted/40", className)} {...p} />;
export const TH = ({ className, ...p }) => <th className={cn("h-9 px-3 font-medium whitespace-nowrap", className)} {...p} />;
export const TD = ({ className, ...p }) => <td className={cn("px-3 py-2.5 align-middle", className)} {...p} />;
