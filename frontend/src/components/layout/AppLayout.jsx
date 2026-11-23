import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { LayoutDashboard, Users, KanbanSquare, Activity, CheckSquare, Sparkles, BarChart3, UserCog, Settings2, ScrollText, KeyRound, Bell, Menu, LogOut, X } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/toast";
import { subscribe } from "@/services/notifications";
import { cn, initials, timeAgo } from "@/lib/utils";
import { Button } from "@/components/ui/button";

/**
 * Sidebar items. `roles` drives RBAC visibility (Section 11.2); omit for "everyone".
 * Server-side checks still apply, this only hides links the user cannot use.
 */
const NAV = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/leads", label: "Leads", icon: Users },
  { to: "/pipeline", label: "Pipeline", icon: KanbanSquare },
  { to: "/activities", label: "Activities", icon: Activity },
  { to: "/tasks", label: "My tasks", icon: CheckSquare },
  { to: "/ai", label: "AI assistant", icon: Sparkles },
  { to: "/account", label: "My account", icon: KeyRound },
  { to: "/reports", label: "Reports", icon: BarChart3, roles: ["Administrator", "Sales Manager"] },
  { group: "Administration", roles: ["Administrator", "Sales Manager"] },
  { to: "/settings", label: "Pipeline & rules", icon: Settings2, roles: ["Administrator", "Sales Manager"] },
  { to: "/users", label: "Users", icon: UserCog, roles: ["Administrator"] },
  { to: "/audit", label: "Audit log", icon: ScrollText, roles: ["Administrator"] },
];

export default function AppLayout() {
  const { user, logout, can } = useAuth();
  const { toast } = useToast();
  const nav = useNavigate();
  const loc = useLocation();
  const [open, setOpen] = useState(false);
  const [bell, setBell] = useState(false);
  const [notes, setNotes] = useState([]);

  useEffect(() => setOpen(false), [loc.pathname]); // close mobile drawer on navigation

  // Real-time reminder notifications (WebSocket in production, see services/notifications.js)
  useEffect(() => subscribe((n) => { setNotes((x) => [n, ...x]); toast({ title: n.title, description: n.body, variant: "info" }); }), [toast]);

  const links = NAV.filter((n) => !n.roles || can(...n.roles));
  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[232px_1fr]">
      {open && <div className="fixed inset-0 z-30 bg-foreground/40 lg:hidden" onClick={() => setOpen(false)} />}
      <aside className={cn("fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r bg-card p-3 transition-transform lg:sticky lg:top-0 lg:h-screen lg:w-auto lg:translate-x-0", open ? "translate-x-0" : "-translate-x-full")}>
        <div className="flex items-center justify-between px-2 py-3">
          <span className="flex items-center gap-2 text-lg font-semibold"><span className="grid size-7 place-items-center rounded-md bg-primary text-sm text-primary-foreground">C</span>Clientra</span>
          <Button variant="ghost" size="icon" className="lg:hidden" onClick={() => setOpen(false)} aria-label="Close menu"><X /></Button>
        </div>
        <nav className="mt-2 flex-1 space-y-0.5 overflow-y-auto">
          {links.map((n, i) => n.group ? <p key={i} className="px-3 pb-1 pt-5 text-xs font-medium text-muted-foreground">{n.group}</p> : (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => cn("flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground", isActive && "bg-accent text-accent-foreground hover:bg-accent")}>
              <n.icon className="size-4" />{n.label}
            </NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-2.5 border-t px-2 pt-3">
          <span className="grid size-8 place-items-center rounded-full bg-accent text-xs font-semibold text-accent-foreground">{initials(user.fullName)}</span>
          <div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">{user.fullName}</p><p className="truncate text-xs text-muted-foreground">{user.role}</p></div>
          <Button variant="ghost" size="icon" aria-label="Sign out" onClick={async () => { await logout(); nav("/login"); }}><LogOut /></Button>
        </div>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b bg-background/90 px-4 backdrop-blur lg:px-8">
          <Button variant="ghost" size="icon" className="lg:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu /></Button>
          <span className="hidden text-sm text-muted-foreground lg:block">AI-Powered Customer Engagement</span>
          <div className="relative">
            <Button variant="ghost" size="icon" onClick={() => setBell(!bell)} aria-label="Notifications"><Bell />{notes.length > 0 && <span className="absolute right-1.5 top-1.5 size-2 rounded-full bg-destructive" />}</Button>
            {bell && (
              <div className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] rounded-lg border bg-card p-2 shadow-lg">
                <div className="flex items-center justify-between px-2 py-1"><p className="text-sm font-semibold">Notifications</p>{notes.length > 0 && <button className="text-xs text-primary" onClick={() => setNotes([])}>Clear all</button>}</div>
                {notes.length === 0 ? <p className="px-2 py-6 text-center text-sm text-muted-foreground">You are all caught up.</p> : notes.map((n) => (
                  <button key={n.id} onClick={() => { setBell(false); nav(`/leads/${n.leadId}`); }} className="block w-full rounded-md px-2 py-2 text-left hover:bg-muted">
                    <p className="text-sm font-medium">{n.title}</p><p className="text-xs text-muted-foreground">{n.body} · {timeAgo(n.at)}</p>
                  </button>))}
              </div>
            )}
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1280px] p-4 lg:p-8"><Outlet /></main>
      </div>
    </div>
  );
}
