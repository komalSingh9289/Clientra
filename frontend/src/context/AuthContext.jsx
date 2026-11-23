import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { authApi } from "@/services/api";
import { TOKEN_KEY } from "@/services/http";

/**
 * Holds the signed-in user + JWT. Synopsis 11.1: stateless JWT, 60-minute expiry.
 * - login():  calls POST /auth/login, stores the token in localStorage
 * - on refresh: validates the stored token via GET /auth/me
 * - auto logout when the token expires (checked every 30 s)
 * - can(...roles): RBAC helper used by routes, sidebar and buttons (UX only; the server enforces real security)
 */
const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

// JWT payloads are base64URL (- and _), and "exp" is in SECONDS, so convert both before comparing with Date.now()
const tokenExpiry = (t) => {
  try {
    const b64 = t.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(b64.padEnd(Math.ceil(b64.length / 4) * 4, "="))).exp * 1000;
  } catch { return 0; }
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(async () => { await authApi.logout(); setUser(null); }, []);

  useEffect(() => {
    const t = localStorage.getItem(TOKEN_KEY);
    if (!t) return setLoading(false);
    authApi.me().then((u) => setUser(u)).catch(() => localStorage.removeItem(TOKEN_KEY)).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!user) return;
    const id = setInterval(() => { if (tokenExpiry(localStorage.getItem(TOKEN_KEY)) < Date.now()) logout(); }, 30000);
    return () => clearInterval(id);
  }, [user, logout]);

  const login = async (email, password) => {
    const { token, user } = await authApi.login(email, password);
    localStorage.setItem(TOKEN_KEY, token); setUser(user);
  };
  const can = (...roles) => !!user && (roles.length === 0 || roles.includes(user.role));

  return <AuthCtx.Provider value={{ user, loading, login, logout, can }}>{children}</AuthCtx.Provider>;
}
