# Clientra: AI-Powered Customer Engagement Platform (MCSP-232)

Full-stack implementation of the synopsis: a **Spring Boot 3 / Java 17 REST backend** and the **React 18 frontend**,
connected through JWT authentication, REST and a WebSocket.

```
clientra/
├── backend/            Spring Boot API (Maven)          -> http://localhost:8080
│   ├── src/main/java/com/clientra/...                   every class is commented with its role in the flow
│   ├── src/main/resources/application*.yml              configuration (H2 default, MySQL profile)
│   └── src/test/java/...                                JUnit 5 unit + integration tests
├── frontend/           React + Vite app                 -> http://localhost:5173
├── docs/schema-mysql.sql                                physical schema with foreign keys (reference)
├── docker-compose.yml  MySQL + backend + frontend in one command
└── README.md
```

## 1. Run it (no database installation needed)

Requirements: **JDK 17+**, **Maven 3.9+**, **Node 18+**.

```bash
# terminal 1: backend (uses a local H2 file database in backend/data/, demo data is loaded on first start)
cd backend
mvn spring-boot:run

# terminal 2: frontend
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** and sign in with a demo account:

| Role                 | Email                  | Password   |
|----------------------|------------------------|------------|
| Administrator        | admin@clientra.demo    | admin123   |
| Sales Manager        | manager@clientra.demo  | manager123 |
| Sales Representative | rep@clientra.demo      | rep123     |

Try this to see the whole system work: log in as the rep, open a lead, log a note such as
*"Frustrated by the delay, considering other vendors"*. The AI marks it **Negative**, flags the lead, re-scores it and pushes a
real-time notification (WebSocket) to the lead's owner.

### With MySQL 8 (the synopsis stack)

```bash
# create nothing by hand: the database is created automatically
SPRING_PROFILES_ACTIVE=mysql DB_HOST=localhost DB_USER=root DB_PASSWORD=yourpassword mvn spring-boot:run
```

### With Docker (MySQL + backend + frontend)

```bash
cp .env.example .env      # then edit the secrets
docker compose up --build  # frontend http://localhost:5173, API http://localhost:8080/api
```

## 2. How a request flows

```
React page -> services/api.js -> http.js (adds "Authorization: Bearer <JWT>")
   -> JwtAuthFilter      verifies signature + expiry, re-loads the user (deactivated users are cut off immediately)
   -> Controller         @PreAuthorize enforces the role (RBAC)
   -> Service            business rules, data scope (a rep sees only own leads), audit trail
   -> Repository         Spring Data JPA, parameterised queries
   -> H2 / MySQL
```
Real-time: `ReminderScheduler` (every 30 s) and `ActivityService` push to `NotificationHub`, which sends JSON over the
WebSocket `ws://localhost:8080/ws/notifications?token=<JWT>`; the bell in the UI listens to it.

Package guide (`backend/src/main/java/com/clientra`): `web/` controllers, `service/` business logic, `ai/` AI engines,
`security/` JWT + RBAC + rate limit, `entity/` + `repository/` data, `dto/` JSON shapes, `ws/` WebSocket, `config/` setup + demo data.

## 3. Synopsis coverage

| Synopsis item | Where it is implemented |
|---|---|
| Module 1 Authentication, JWT 60 min, RBAC, bcrypt (12), token validation | `security/*`, `AuthService`, `SecurityConfig`, `@PreAuthorize` on controllers |
| Module 2 Lead CRUD, validation, duplicate detection, CSV import | `LeadService`, `CsvUtil`, `POST /api/leads/import` (+ `/import/csv` multipart) |
| CSV and PDF export | `ExportService` (OpenPDF), `GET /api/leads/export`, `GET /api/reports/export` |
| Module 3 Kanban pipeline, configurable stages, metrics | `StageService`, `PATCH /leads/{id}/stage`, `DashboardService` (value per stage, days in stage) |
| Module 4 Activity log + automatic sentiment on every note | `ActivityService` |
| Module 5 (a) Smart lead scoring | `ai/LeadScorer` (frequency, recency, sentiment, deal value) |
| (b) NLP message generation | `AiService.draft` -> LLM (OpenAI/Gemini) with `ai/MessageGenerator` fallback |
| (c) Sentiment analysis | `ai/SentimentAnalyzer` |
| (d) Interaction summaries | `AiService.summary` -> LLM with `ai/InteractionSummarizer` fallback |
| Predictive follow-up timing | `ai/FollowUpPredictor` |
| AI dashboard insights | `ai/InsightsGenerator` |
| Module 6 Reminders, WebSocket alerts, e-mail for due/overdue | `ReminderService`, `ReminderScheduler`, `ws/*`, `EmailService` |
| Module 7 KPIs, trends, filtered reports | `DashboardService`, `ReportService` |
| Module 8 Users, roles, stages, e-mail rules, audit log | `UserService`, `SettingsService`, `AuditService` |
| 11.5 SQL-injection prevention | JPA parameterised queries only, `@Valid` bean validation |
| 11.6 Login rate limit 5 / 15 min / IP + immutable audit log | `LoginRateLimiter`, `AuditLog` (`@Immutable`, no update/delete code) |
| 11.4 HTTPS | set `CLIENTRA_REQUIRE_HTTPS=true` behind a TLS proxy |
| 7.6 Tables | entities in `entity/`, reference DDL in `docs/schema-mysql.sql` |
| 10 Testing | `backend/src/test` (JUnit 5, Spring MockMvc) |

### REST API (all under `/api`, JSON; everything except login needs the Bearer token)

| Area | Endpoints | Roles |
|---|---|---|
| Auth | `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`, `POST /auth/change-password` | all |
| Leads | `GET /leads` (filters: search, stageId, priority, source, assignedTo, from, to), `GET/PUT/DELETE /leads/{id}`, `POST /leads[?force=true]`, `PATCH /leads/{id}/stage`, `POST /leads/import`, `POST /leads/import/csv`, `GET /leads/export?format=csv\|pdf`, `POST /leads/export-log` | all (reps: own leads only) |
| Pipeline | `GET /pipeline/stages`; `POST`, `PUT /{id}`, `PUT /order`, `DELETE /{id}` | read: all, change: Admin + Manager |
| Activities | `GET /activities?leadId=`, `POST /activities` | all (data scope applies) |
| Deals | `GET /deals?leadId=`, `POST /deals`, `PATCH /deals/{id}` | all (data scope applies) |
| Reminders | `GET /reminders`, `POST /reminders`, `PATCH /reminders/{id}` | all (reps: own) |
| AI | `POST /ai/draft-message`, `/ai/summary`, `/ai/lead-score`, `/ai/sentiment`; `GET /ai/follow-up-time/{leadId}`, `/ai/logs` | all |
| Dashboard / reports | `GET /dashboard`; `GET /reports`, `GET /reports/export` | dashboard: all, reports: Admin + Manager |
| Users | `GET/POST /users`, `PUT /users/{id}`, `PATCH /users/{id}/active`, `GET /roles`; `GET /users/assignable` | Admin (assignable: all) |
| Audit / settings | `GET /audit-logs`; `GET/PUT /settings/notification-rules` | audit: Admin; rules: read Admin + Manager, save Admin |

Errors always look like `{ "status": 409, "message": "...", "code": "DUPLICATE" }`.

## 4. Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `CLIENTRA_JWT_SECRET` | dev value | **Change in production.** At least 32 characters (`openssl rand -base64 48`) |
| `CLIENTRA_SEED_DEMO` | `true` | `false` = no demo data, only the admin from `CLIENTRA_ADMIN_EMAIL` / `CLIENTRA_ADMIN_PASSWORD` |
| `CLIENTRA_CORS_ORIGINS` | `http://localhost:5173` | Allowed browser origin(s), comma separated (also used for the WebSocket) |
| `CLIENTRA_REQUIRE_HTTPS` | `false` | `true` redirects HTTP to HTTPS (use behind a TLS proxy) |
| `CLIENTRA_AI_PROVIDER` / `_API_KEY` / `_MODEL` | `none` | `openai` or `gemini` switches message drafting and summaries to the LLM. Without a key the built-in engine is used |
| `CLIENTRA_MAIL_ENABLED`, `MAIL_HOST/PORT/USERNAME/PASSWORD` | off | SMTP for notification e-mails; when off, e-mails are only written to the log |
| `DB_HOST/PORT/NAME/USER/PASSWORD` | localhost / root | MySQL settings (profile `mysql`) |

Frontend: `VITE_API_BASE_URL` (default `http://localhost:8080/api`), see `frontend/.env.example`.

## 5. Tests

```bash
cd backend && mvn test
```
Unit tests: sentiment, lead scoring, CSV parsing/injection guard, login rate limit, JWT (expired, tampered, foreign key).
`SecurityIntegrationTest` boots the whole app on an in-memory DB and checks 401 / 403, the rep-vs-manager data scope and the dashboard.

## 6. Honest notes: where this differs from the synopsis

* **Lead scoring** is a transparent weighted model written in Java, not a scikit-learn model in a separate Python service.
  It uses exactly the signals the synopsis lists. Replacing it with a trained model only means changing `ai/LeadScorer`.
* **Sentiment analysis** is a lexicon classifier (fast, offline, deterministic), not a trained neural model.
* **Message generation / summaries** call OpenAI or Gemini when you configure a key (lead names and notes are then sent to that provider);
  otherwise, or if the call fails, the built-in rule-based engine answers, so the app never depends on an external service.
* **MongoDB** (optional in the synopsis) is not used: AI logs live in MySQL tables `tbl_ai_logs` / `tbl_ai_responses`.
* The synopsis columns named `timestamp` are stored as `logged_at` (`timestamp` is a SQL keyword). A few extra columns support features the UI needs (listed at the top of `docs/schema-mysql.sql`).
* Foreign keys are enforced by the services; the reference SQL adds database-level FK constraints if you want them (the app's own `ddl-auto=update` does not create them).
* The Won and Lost stages cannot be deleted (reporting needs them) but can be renamed; new stages are always "in progress" stages.
* Conversion rate = won / (won + lost) deals; the monthly trend uses the date each deal was closed.
* Demo data includes some historical closed deals so the trend chart is not empty. Never run production with `CLIENTRA_SEED_DEMO=true`.

## 7. Production checklist

1. Set a long random `CLIENTRA_JWT_SECRET` and `CLIENTRA_SEED_DEMO=false`; change the bootstrap admin password after the first login (My account page).
2. Run behind HTTPS (nginx/Caddy) and set `CLIENTRA_REQUIRE_HTTPS=true`, `CLIENTRA_CORS_ORIGINS=https://your-domain`.
3. Use MySQL (profile `mysql`) and back it up. The login rate limiter and WebSocket registry are in-memory, so run a single backend instance (or add Redis for several).
4. The WebSocket passes the JWT in the URL query; do not log full request URLs on your proxy.
