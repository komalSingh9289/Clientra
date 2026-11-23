/**
 * Real-time notifications (Module 6) over a WebSocket, as the synopsis specifies.
 * The server (ws/NotificationWebSocketHandler) pushes { id, title, body, leadId, at } whenever
 *   - a follow-up reminder becomes due, or
 *   - an interaction with negative sentiment is logged on one of your leads.
 * The JWT is passed as ?token= because browsers cannot set headers on a WebSocket.
 * If the connection drops (server restart, network), it reconnects with exponential back-off.
 * Notifications already shown are remembered by id, so a reconnect never shows them twice.
 */
import { TOKEN_KEY } from "./http";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const WS_URL = import.meta.env.VITE_WS_URL || API.replace(/^http/, "ws").replace(/\/api\/?$/, "") + "/ws/notifications";

export function subscribe(onNotify) {
  const seen = new Set();
  let ws, timer, retry = 0, closed = false;

  const connect = () => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (closed || !token) return;
    ws = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`);
    ws.onopen = () => { retry = 0; };
    ws.onmessage = (e) => {
      try {
        const n = JSON.parse(e.data);
        if (seen.has(n.id)) return;
        seen.add(n.id);
        onNotify(n);
      } catch { /* ignore malformed frames */ }
    };
    ws.onclose = () => { if (!closed) timer = setTimeout(connect, Math.min(30000, 1000 * 2 ** retry++)); };
  };

  connect();
  return () => { closed = true; clearTimeout(timer); if (ws) ws.close(); };   // unsubscribe
}
