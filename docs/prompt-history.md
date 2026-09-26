# Prompt History — Support Ticket Management System

> Chronological log of the key prompts that built this system. Each entry records the prompt text (reconstructed), the files it produced, and how the output was verified. New file only — `SPEC.md` and `docs/SPEC_CONSOLIDATED.md` were not modified to create it.

## 1. Spec-first — write SPEC before any code

**Prompt text:**

> "Create a minimal support-ticket tracker spec first, before any code. Stack: Java 21 + Spring Boot 3.2.x + H2 file DB on :8080, Vite + React on the frontend. Requirements F1–F11, domain model, backend-enforced state machine, API design, validation rules, UI/UX, H2 persistence, test strategy, acceptance criteria, file map. Save as SPEC.md, the source of truth."

**Output files:**

- `SPEC.md` (v1 source of truth, §§1–11)
- `README.md` (run instructions)

**Verification:**

- Read `SPEC.md` end-to-end; checked F1–F11, state table, and file map were complete before scaffolding anything.

## 2. Backend scaffold — domain → repos → service → web

**Prompt text:**

> "Scaffold the backend from SPEC §§2–5, smallest diffs first: domain (Ticket, TicketStatus with canTransitionTo(), Priority, Comment), repositories (TicketRepository, CommentRepository), service (TicketService + changeStatus enforcement, NotFoundException, InvalidStatusTransitionException), web (TicketController, GlobalExceptionHandler, DTOs TicketRequest/TicketUpdateRequest/StatusChangeRequest/CommentRequest/TicketResponse/CommentResponse), config (application.properties with H2 file DB, H2 console, CORS)."

**Output files:**

- `backend/pom.xml`
- `backend/src/main/java/com/example/ticketai/domain/{Ticket,TicketStatus,Priority,Comment}.java`
- `backend/src/main/java/com/example/ticketai/repository/{TicketRepository,CommentRepository}.java`
- `backend/src/main/java/com/example/ticketai/service/{TicketService,NotFoundException,InvalidStatusTransitionException}.java`
- `backend/src/main/java/com/example/ticketai/web/{TicketController,GlobalExceptionHandler}.java`
- `backend/src/main/java/com/example/ticketai/web/dto/{TicketRequest,TicketUpdateRequest,StatusChangeRequest,CommentRequest,TicketResponse,CommentResponse}.java`
- `backend/src/main/resources/application.properties`

**Verification:**

- `cd backend && mvn -q compile` succeeds; `mvn spring-boot:run` starts on `:8080`, `/h2-console` reachable.

## 3. Frontend scaffold — single-page app, no router (legacy)

**Prompt text:**

> "Scaffold the frontend from SPEC §6: single-page app with no router (selected-state toggles list/detail). Files: App.jsx (create form, list + search/filter, detail, status buttons, edit form, comments, error banner), api.js (fetch wrapper + STATUSES/PRIORITIES/allowedNext), main.jsx, styles.css, vite.config.js with /api proxy to :8080."

**Output files:**

- `frontend/package.json`, `frontend/vite.config.js`, `frontend/index.html`
- `frontend/src/{main.jsx,App.jsx,api.js,styles.css}`

**Verification:**

- `cd frontend && npm install && npm run dev` serves the UI; manual create → list → detail → comment walkthrough against the running backend.

## 4. Verification — 9 tests + persistence

**Prompt text:**

> "Verify the build: write 9 state-machine integration tests (SpringBootTest + MockMvc, @Transactional, in-memory H2) — 3 valid lifecycles, 4 invalid → 422, 2 validation → 400. Run mvn test (expect 9 green). Then verify persistence: create a ticket, stop the backend, restart, confirm it is still listed (H2 file in backend/data/)."

**Output files:**

- `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java`
- `backend/src/test/resources/application.properties` (in-memory H2 override)

**Verification:**

- `cd backend && mvn test` → 9 tests green.
- Create → stop → restart → ticket still listed; `backend/data/ticketdb.mv.db` present.
- Caught the Surefire `*IT`-vs-`*Test` skip issue here (see `docs/AI_MISTAKES.md`): a 0-tests green build exposed it.

## 5. PR split — 5 reviewable slices + backup branch

**Prompt text:**

> "Split the work into 5 reviewable slices (entities → repos → service → web → frontend), each independently explainable per the spec-driven workflow. Snapshot everything on branch backup/main-work-20260926."

**Output files:**

- (No new source files; git history organization.)
- Branch `backup/main-work-20260926` as the snapshot.

**Verification:**

- `git branch backup/main-work-20260926`; `git log` shows the slice commits; each slice explainable against SPEC sections.

## 6. Postman — curls + collection

**Prompt text:**

> "Verify all 8 endpoints with curl (create, list/filter/search, detail, update, status change valid + invalid → 422, add comment, list comments, delete → 204) and save them as the Postman collection postman/TicketAI.postman_collection.json."

**Output files:**

- `postman/TicketAI.postman_collection.json`
- Curl reference in `docs/BACKEND.md#api--curl-examples`

**Verification:**

- Each curl run against `localhost:8080` returned the expected status (201/200/422/204); collection imports cleanly into Postman.

## 7. Repo split — standalone frontend on :5174

**Prompt text:**

> "Move the UI to the standalone repo ticket_frontend/ticket_frontend_ai (sibling of ticket-ai) on dev port :5174 with /api proxy to :8080. Freeze the legacy frontend/ copy inside the backend repo — do not extend it. Fix the duplicate-Vite 5173/5174 proxy issue and the nested-path + server.port 8081 confusion."

**Output files:**

- `ticket_frontend/ticket_frontend_ai/package.json`, `vite.config.js` (port 5174, `/api` proxy), `index.html`
- `ticket_frontend/ticket_frontend_ai/src/{main.jsx,App.jsx,api.js,styles.css}` (router shell at this stage)

**Verification:**

- `ls` confirms the exact path (no extra nesting); `application.properties` reads `server.port=8080`.
- `curl -i localhost:5174/api/tickets` returns JSON (not HTML); `ss -tlnp` shows a single Vite listener on 5174.
- Details of what went wrong here: see `docs/AI_MISTAKES.md` (duplicate Vite + nested path + port override).

## 8. Pages + pagination — router + envelope

**Prompt text:**

> "Upgrade the standalone frontend to react-router-dom@6 with routes /, /create, /tickets/:id, /tickets/:id/edit (fallback * → list). Split into pages/ListPage (filters + table + pagination: page/size selector 5/10/20, Prev/numbered/Next, counts label), CreatePage, DetailPage (all five status buttons + allowedNext hint + comments + delete), EditPage, shared ErrorBanner. Backend: add paged list mode — any page/size/sort param returns the {content, page, size, totalElements, totalPages} envelope; no params returns the legacy bare array. Extend the API client with toPage() normalization."

**Output files:**

- `backend`: dual-mode list in `TicketController` (+ `parseSort`), pageable repository variants, `TicketService.listPaged`
- `ticket_frontend/ticket_frontend_ai/src/{api.js (toPage, PAGE_SIZES),App.jsx (routes),pages/ListPage.jsx,pages/CreatePage.jsx,pages/DetailPage.jsx,pages/EditPage.jsx}`

**Verification:**

- `curl 'localhost:8080/api/tickets?page=0&size=5'` returns the envelope; bare `curl localhost:8080/api/tickets` still returns an array.
- UI: change page size 5/10/20, Prev/numbered/Next, counts update; `npm run build` clean.

## 9. Consolidated spec — single self-contained build contract

**Prompt text:**

> "Consolidate everything into docs/SPEC_CONSOLIDATED.md — a single self-contained file an agent can use to rebuild backend + frontend from scratch (goal/scope F1–F13, stack/ports, repos + file maps, domain model, state machine, API contract with pagination envelope + curl per endpoint, validation, persistence/config, CORS, frontend spec with routes + pagination UX, 9 tests, acceptance criteria, build order, verification). Do not modify SPEC.md; treat it as v1 history."

**Output files:**

- `docs/SPEC_CONSOLIDATED.md` (§§1–14)
- Companion summaries `docs/BACKEND.md`, `docs/FRONTEND.md`, `docs/CHAT_HISTORY.md`

**Verification:**

- Diffed consolidated spec against `SPEC.md` + working tree: F12/F13 (delete, pagination), standalone repo layout, and `:5174` port all present; `SPEC.md` untouched (`git status` clean for it).

## 10. Demo guide — manual walkthrough

**Prompt text:**

> "Write the demo/verification walkthrough: backend tests (mvn test, 9 green), backend run, frontend run (backend up first), then the manual script — create → list → detail → edit → comments; OPEN→IN_PROGRESS→RESOLVED→CLOSED then CLOSED→OPEN (422 banner) and OPEN→RESOLVED on a fresh ticket (422); search + filter + Reset; pagination sizes and Prev/numbered/Next; restart persistence; delete → 204 → 404 on direct GET. Include H2 console + devtools debug aids."

**Output files:**

- Verification section in `docs/SPEC_CONSOLIDATED.md` (§14)
- `spec/test-strategy.md` (this task; derived from it)

**Verification:**

- Ran the full script top-to-bottom: 9 green, `npm run build` clean, all six walkthrough steps pass, data survives restart.
