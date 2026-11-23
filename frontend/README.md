# Clientra frontend (React 18 + Vite + Tailwind)

Talks to the Spring Boot backend in `../backend`. See the root `README.md` for the full guide.

```bash
npm install
npm run dev        # http://localhost:5173  (backend expected at http://localhost:8080)
npm run build      # production bundle in dist/
```

Set `VITE_API_BASE_URL` (see `.env.example`) if the API is not at `http://localhost:8080/api`.

* `src/services/api.js`: one function per backend endpoint (the only place that knows URLs)
* `src/services/http.js`: fetch wrapper (JWT header, error mapping, 401 handling)
* `src/services/notifications.js`: WebSocket client for real-time alerts
* `src/context/AuthContext.jsx`: login state, token expiry, role helper `can(...)`
