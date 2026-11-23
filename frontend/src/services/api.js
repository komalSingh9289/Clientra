/**
 * API layer: one function per backend endpoint (synopsis Modules 1-8).
 * The React pages only ever call these functions, so nothing else in the UI knows about URLs.
 * Each function returns the parsed JSON and throws an Error (with .message / .code) on failure.
 * Backend: clientra-backend (Spring Boot), controllers are in backend/src/main/java/com/clientra/web.
 */
import { http, TOKEN_KEY } from "./http";

const num = (v) => (v === "" || v == null ? null : Number(v));

// ---------------------------------------------------------------- Module 1: authentication
export const authApi = {
  login: (email, password) => http.post("/auth/login", { email, password }),   // -> { token, user }
  me: () => http.get("/auth/me"),
  changePassword: (currentPassword, newPassword) => http.post("/auth/change-password", { currentPassword, newPassword }),                                              // restores the session on page reload
  async logout() {
    // JWT is stateless: the server only records the event; the browser drops the token.
    try { await http.post("/auth/logout"); } catch { /* token may already be expired */ }
    localStorage.removeItem(TOKEN_KEY);
  },
};

// ---------------------------------------------------------------- Module 2: leads
const leadBody = (d) => ({
  fullName: d.fullName, email: d.email || "", phone: d.phone || "", company: d.company || "", source: d.source || "",
  stageId: num(d.stageId), assignedTo: num(d.assignedTo),
});
export const leadsApi = {
  list: (f = {}) => http.get("/leads", f),                                     // search, stageId, priority, source, assignedTo, from, to
  get: (id) => http.get(`/leads/${id}`),
  // 409 + code "DUPLICATE" when the e-mail/phone exists, unless force = true
  create: (d, { force = false } = {}) => http.post(`/leads${force ? "?force=true" : ""}`, leadBody(d)),
  update: (id, d) => http.put(`/leads/${id}`, leadBody(d)),
  updateStage: (id, stageId) => http.patch(`/leads/${id}/stage`, { stageId: Number(stageId) }),
  remove: (id) => http.delete(`/leads/${id}`),
  bulkImport: (rows) => http.post("/leads/import", rows),                      // -> { imported, skipped: [{ row, reason }] }
  logExport: (format) => http.post("/leads/export-log", { format }),           // the file is built in the browser; this records it in the audit log
};

// ---------------------------------------------------------------- Module 3: pipeline stages
export const stagesApi = {
  list: () => http.get("/pipeline/stages"),
  create: (stageName) => http.post("/pipeline/stages", { stageName }),
  rename: (id, stageName) => http.put(`/pipeline/stages/${id}`, { stageName }),
  reorder: (ids) => http.put("/pipeline/stages/order", { ids }),
  remove: (id) => http.delete(`/pipeline/stages/${id}`),
};

// ---------------------------------------------------------------- Module 4: activities
export const activitiesApi = {
  list: ({ leadId } = {}) => http.get("/activities", { leadId }),
  // -> { activity, sentiment, flagged, score }  (the server runs sentiment analysis + re-scoring)
  create: ({ leadId, type, description }) => http.post("/activities", { leadId: Number(leadId), type, description }),
};

// ---------------------------------------------------------------- deals
export const dealsApi = {
  byLead: (leadId) => http.get("/deals", { leadId }),
  create: (d) => http.post("/deals", { leadId: Number(d.leadId), amount: Number(d.amount), closingDate: d.closingDate || null, notes: d.notes || "" }),
  setStatus: (id, status) => http.patch(`/deals/${id}`, { status }),
};

// ---------------------------------------------------------------- Module 6: reminders
export const remindersApi = {
  list: () => http.get("/reminders"),
  // the date-time picker gives local time; send it as an absolute UTC instant
  create: (d) => http.post("/reminders", {
    leadId: Number(d.leadId), reminderTime: new Date(d.reminderTime).toISOString(), message: d.message, priority: d.priority || "Medium",
  }),
  setStatus: (id, status) => http.patch(`/reminders/${id}`, { status }),
};

// ---------------------------------------------------------------- Module 5: AI
export const aiApi = {
  draft: (leadId, tone) => http.post("/ai/draft-message", { leadId: Number(leadId), tone }),   // -> { subject, body }
  summary: (leadId) => http.post("/ai/summary", { leadId: Number(leadId) }),
  score: (leadId) => http.post("/ai/lead-score", { leadId: Number(leadId) }),                  // -> { score, priority, factors }
  followUp: (leadId) => http.get(`/ai/follow-up-time/${leadId}`),                              // -> { day, window, reason }
  sentiment: (text) => http.post("/ai/sentiment", { text }),                                   // -> { label, confidence, ... }
  logs: () => http.get("/ai/logs"),
};

// ---------------------------------------------------------------- Module 7: dashboard & reports
export const reportsApi = {
  dashboard: () => http.get("/dashboard"),
  report: (f = {}) => http.get("/reports", f),                                 // from, to, stageId, source, assignedTo (managers/admins)
};

// ---------------------------------------------------------------- Module 8: administration
export const usersApi = {
  list: () => http.get("/users"),
  create: (d) => http.post("/users", { fullName: d.fullName, email: d.email, roleId: Number(d.roleId), password: d.password }),
  update: (id, d) => http.put(`/users/${id}`, { fullName: d.fullName, email: d.email, roleId: Number(d.roleId), password: d.password || null }),
  setActive: (id, active) => http.patch(`/users/${id}/active`, { active }),
  roles: () => http.get("/roles"),
  assignable: () => http.get("/users/assignable"),                             // active users, for "assign to" pickers (all roles)
};
export const auditApi = { list: (f = {}) => http.get("/audit-logs", f) };      // action, from, to (administrators)
export const settingsApi = {
  getRules: () => http.get("/settings/notification-rules"),
  saveRules: (r) => http.put("/settings/notification-rules", r),
};
