/**
 * HTTP client for the Clientra Spring Boot backend.
 * Every call:
 *   - goes to VITE_API_BASE_URL (default http://localhost:8080/api)
 *   - carries the JWT saved at login in the Authorization header (synopsis 11.1)
 *   - on failure throws an Error whose message is the server's message and which also has
 *       .status (HTTP status) and .code (e.g. "DUPLICATE") so the UI can react to special cases
 * A 401 anywhere (expired / tampered token) clears the token and sends the user to /login.
 */
const BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
export const TOKEN_KEY = "clientra_token";

async function request(method, path, { body, params } = {}) {
  const qs = params ? "?" + new URLSearchParams(Object.entries(params).filter(([, v]) => v !== "" && v != null)) : "";
  let res;
  try {
    res = await fetch(`${BASE}${path}${qs}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        ...(localStorage.getItem(TOKEN_KEY) ? { Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw Object.assign(new Error("Cannot reach the server. Check that the backend is running."), { status: 0 });
  }
  // A wrong password on the login form is also a 401, but must NOT redirect: just show the message.
  if (res.status === 401 && path !== "/auth/login") {
    localStorage.removeItem(TOKEN_KEY);
    window.location.assign("/login");
  }
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw Object.assign(new Error(data.message || res.statusText || "Request failed"), { status: res.status, code: data.code });
  }
  return res.status === 204 ? null : res.json();
}

export const http = {
  get: (p, params) => request("GET", p, { params }),
  post: (p, body) => request("POST", p, { body: body ?? {} }),
  put: (p, body) => request("PUT", p, { body }),
  patch: (p, body) => request("PATCH", p, { body }),
  delete: (p) => request("DELETE", p),
};
