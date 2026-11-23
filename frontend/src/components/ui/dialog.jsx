import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

export const Dialog = DialogPrimitive.Root;
export const DialogTrigger = DialogPrimitive.Trigger;
export const DialogClose = DialogPrimitive.Close;

/** Modal. On phones it becomes a near-full-height sheet; on larger screens a centred panel. */
export const DialogContent = ({ className, children, ...props }) => (
  <DialogPrimitive.Portal>
    <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-foreground/40" />
    <DialogPrimitive.Content
      className={cn(
        "fixed z-50 grid gap-4 bg-card p-5 shadow-lg focus:outline-none",
        "inset-x-0 bottom-0 max-h-[92vh] overflow-y-auto rounded-t-xl border",
        "sm:inset-auto sm:left-1/2 sm:top-1/2 sm:w-full sm:max-w-lg sm:-translate-x-1/2 sm:-translate-y-1/2 sm:rounded-lg",
        className
      )}
      {...props}
    >
      {children}
      <DialogPrimitive.Close className="absolute right-4 top-4 rounded-md p-1 text-muted-foreground hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring" aria-label="Close">
        <X className="size-4" />
      </DialogPrimitive.Close>
    </DialogPrimitive.Content>
  </DialogPrimitive.Portal>
);
export const DialogHeader = ({ className, ...p }) => <div className={cn("space-y-1 pr-6", className)} {...p} />;
export const DialogTitle = ({ className, ...p }) => <DialogPrimitive.Title className={cn("text-base font-semibold", className)} {...p} />;
export const DialogDescription = ({ className, ...p }) => <DialogPrimitive.Description className={cn("text-sm text-muted-foreground", className)} {...p} />;
export const DialogFooter = ({ className, ...p }) => <div className={cn("flex flex-col-reverse gap-2 sm:flex-row sm:justify-end", className)} {...p} />;
