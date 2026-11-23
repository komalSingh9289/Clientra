import * as React from "react";
import { cn } from "@/lib/utils";

const fieldBase =
  "flex w-full rounded-md border border-input bg-card px-3 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50";

export const Input = React.forwardRef(({ className, ...props }, ref) => (
  <input ref={ref} className={cn(fieldBase, "h-9 py-1", className)} {...props} />
));
export const Textarea = React.forwardRef(({ className, ...props }, ref) => (
  <textarea ref={ref} className={cn(fieldBase, "min-h-[84px] py-2", className)} {...props} />
));
/** Native <select> styled like shadcn's Select: best touch/mobile UX and trivial to wire to forms. */
export const Select = React.forwardRef(({ className, children, ...props }, ref) => (
  <select ref={ref} className={cn(fieldBase, "h-9 py-1 pr-8", className)} {...props}>{children}</select>
));
export const Label = ({ className, ...props }) => (
  <label className={cn("text-sm font-medium leading-none", className)} {...props} />
);
/** Label + control + error message in one block */
export const Field = ({ label, error, hint, children, className }) => (
  <div className={cn("space-y-1.5", className)}>
    {label && <Label>{label}</Label>}
    {children}
    {hint && !error && <p className="text-xs text-muted-foreground">{hint}</p>}
    {error && <p className="text-xs text-destructive">{error}</p>}
  </div>
);
