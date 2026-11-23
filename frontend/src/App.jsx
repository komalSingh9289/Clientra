import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import AppLayout from "@/components/layout/AppLayout";
import { Loading } from "@/components/shared/ui-bits";
import Login from "@/pages/Login";
import Dashboard from "@/pages/Dashboard";
import Leads from "@/pages/Leads";
import LeadDetail from "@/pages/LeadDetail";
import Pipeline from "@/pages/Pipeline";
import Activities from "@/pages/Activities";
import Tasks from "@/pages/Tasks";
import AIAssistant from "@/pages/AIAssistant";
import Reports from "@/pages/Reports";
import Settings from "@/pages/Settings";
import Users from "@/pages/Users";
import AuditLog from "@/pages/AuditLog";
import Account from "@/pages/Account";

/** Route guard: redirects to /login when signed out, and to "/" when the role is not allowed (RBAC, Section 11.2). */
function Guard({ roles, children }) {
  const { user, loading, can } = useAuth();
  if (loading) return <Loading className="min-h-screen" />;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !can(...roles)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const { user } = useAuth();
  const MGR = ["Administrator", "Sales Manager"];
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route element={<Guard><AppLayout /></Guard>}>
        <Route index element={<Dashboard />} />
        <Route path="leads" element={<Leads />} />
        <Route path="leads/:id" element={<LeadDetail />} />
        <Route path="pipeline" element={<Pipeline />} />
        <Route path="activities" element={<Activities />} />
        <Route path="tasks" element={<Tasks />} />
        <Route path="account" element={<Account />} />
        <Route path="ai" element={<AIAssistant />} />
        <Route path="reports" element={<Guard roles={MGR}><Reports /></Guard>} />
        <Route path="settings" element={<Guard roles={MGR}><Settings /></Guard>} />
        <Route path="users" element={<Guard roles={["Administrator"]}><Users /></Guard>} />
        <Route path="audit" element={<Guard roles={["Administrator"]}><AuditLog /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
