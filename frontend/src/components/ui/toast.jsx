import { createContext, useCallback, useContext, useState } from "react";
import { CheckCircle2, AlertCircle, Info } from "lucide-react";
import { cn } from "@/lib/utils";

/** Minimal toast system: const { toast } = useToast(); toast({ title: "Lead saved" }) */
const ToastCtx = createContext(() => {});
export const useToast = () => ({ toast: useContext(ToastCtx) });

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const toast = useCallback(({ title, description, variant = "default" }) => {
    const id = Math.random().toString(36).slice(2);
    setItems((x) => [...x, { id, title, description, variant }]);
    setTimeout(() => setItems((x) => x.filter((i) => i.id !== id)), 4000);
  }, []);
  const Icon = { default: CheckCircle2, error: AlertCircle, info: Info };
  return (
    <ToastCtx.Provider value={toast}>
      {children}
      <div className="pointer-events-none fixed inset-x-3 bottom-3 z-[60] flex flex-col items-end gap-2 sm:inset-x-auto sm:right-4" aria-live="polite">
        {items.map((t) => {
          const I = Icon[t.variant] || Info;
          return (
            <div key={t.id} className="pointer-events-auto flex w-full max-w-sm items-start gap-2.5 rounded-lg border bg-card p-3 shadow-lg">
              <I className={cn("mt-0.5 size-4 shrink-0", t.variant === "error" ? "text-destructive" : t.variant === "info" ? "text-primary" : "text-success")} />
              <div className="text-sm"><p className="font-medium">{t.title}</p>{t.description && <p className="text-muted-foreground">{t.description}</p>}</div>
            </div>
          );
        })}
      </div>
    </ToastCtx.Provider>
  );
}
