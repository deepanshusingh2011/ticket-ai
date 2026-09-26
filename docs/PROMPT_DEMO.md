# PROMPT_DEMO.md — Demonstration Guide (Copy-Paste Prompt Journey)

> Purpose: replay the entire build as a live demo, one prompt at a time.
> Each stage below is self-contained: paste the **Exact prompt** into a fresh agent,
> observe the **Expected output**, run **How to verify**, and read the **What to say** line aloud.
>
> Source of truth for what was built: `docs/SPEC_CONSOLIDATED.md` (single build contract).
> History: `SPEC.md` (v1 spec), `docs/CHAT_HISTORY.md` (chronological log),
> `docs/BACKEND.md`, `docs/FRONTEND.md` (references). This file adds nothing new — it only scripts the replay.
>
> Repos used in the demo:
> - Repo A (backend + docs): `/home/deepanshu/Videos/Ticketsystem/ticket/ticket-ai` (branch `backup/main-work-20260926`)
> - Repo B (frontend standalone): `/home/deepanshu/Videos/Ticketsystem/ticket/ticket_frontend/ticket_frontend_ai`
>
> Suggested total time: ~20–30 min (2–3 min per stage). Nothing here commits or pushes.

## 0. Deliverables inventory (show this table first)

| # | Deliverable | Path / location | What it proves |
|---|-------------|-----------------|----------------|
| 1 | V1 spec | `SPEC.md` | Spec-first workflow: requirements F1–F11, state machine, API, validation, UI/UX before any code |
| 2 | Consolidated build spec | `docs/SPEC_CONSOLIDATED.md` | Single file that rebuilds everything (backend + frontend + pagination + routes) |
| 3 | Backend domain | `backend/src/main/java/com/example/ticketai/domain/{Ticket,TicketStatus,Priority,Comment}.java` | Entities, `TicketStatus.canTransitionTo()`, defaults OPEN/MEDIUM, blank→null |
| 4 | Backend repositories | `backend/src/main/java/com/example/ticketai/repository/{TicketRepository,CommentRepository}.java` | JPA + status filter + case-insensitive title/description search + pageable variants |
| 5 | Backend service | `backend/src/main/java/com/example/ticketai/service/{TicketService,NotFoundException,InvalidStatusTransitionException}.java` | 8 ops (create/list/listPaged/get/update/changeStatus/addComment+listComments/delete), 422 enforcement, same-status no-op |
| 6 | Backend web layer | `backend/src/main/java/com/example/ticketai/web/{TicketController,GlobalExceptionHandler}.java` + `web/dto/*.java` (6 DTOs) | 9 endpoints, dual-mode list (bare array vs pagination envelope), 400/404/422 envelopes, CORS 5174/5173/3000 |
| 7 | Backend config | `backend/src/main/resources/application.properties` + `backend/src/test/resources/application.properties` | H2 file DB (`./data/ticketdb`, survives restart) vs in-memory H2 for tests |
| 8 | State-machine tests | `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java` | 9 tests: 3 valid lifecycles, 4 invalid→422, 2 validation→400 |
| 9 | Frontend standalone app | `ticket_frontend/ticket_frontend_ai/` (`package.json`, `vite.config.js`, `index.html`, `src/{main.jsx,App.jsx,api.js,styles.css,pages/ListPage.jsx,pages/CreatePage.jsx,pages/DetailPage.jsx,pages/EditPage.jsx}`) | Routes `/`, `/create`, `/tickets/:id`, `/tickets/:id/edit`; pagination UX; status buttons proving 422 |
| 10 | Postman collection | `postman/TicketAI.postman_collection.json` | All 9 endpoints as runnable requests |
| 11 | Reference docs | `docs/{BACKEND,FRONTEND,CHAT_HISTORY}.md` | Backend/frontend references + chronological build log |
| 12 | Snapshot branch | `backup/main-work-20260926` | Point-in-time backup of the working tree |
| 13 | This demo script | `docs/PROMPT_DEMO.md` | The file you are reading |

State machine under demo (backend-enforced):

```text
OPEN ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
 │            │
 └─→ CANCELLED └─→ CANCELLED
```

Pagination envelope (paged mode):

```json
{ "content": [ { "id": 1, "title": "…" } ], "page": 0, "size": 10, "totalElements": 42, "totalPages": 5 }
```

---

## Stage (a) — Spec-first prompt (produced SPEC.md)

**Goal:** Write the spec before any code, so every later prompt has a contract to build against.

**Exact prompt to paste:**

```text
Create SPEC.md for a minimal support-ticket tracker for small co-located teams (no auth in v1).
Stack: backend Java 21 + Spring Boot 3.2.x + H2 file-based on :8080; frontend Vite + React 18.
Requirements F1-F11: create ticket (title, description, priority, assignee); list; detail incl.
comments; update title/description/priority/assignee; status changes ONLY via allowed transitions;
add comments; keyword search (title+description, case-insensitive partial); filter by status;
combine filter+search; persist across restarts (H2 file); validation with meaningful UI errors.
Domain: Ticket (id, title 1..200 required, description 1..4000 required, status default OPEN,
priority default MEDIUM, assignee optional blank->null, createdAt/updatedAt Instants,
comments OneToMany cascade ALL orphanRemoval), Comment (ticket FK required, body 1..2000 required,
author optional blank->null, createdAt). Statuses OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED;
allowed: OPEN->IN_PROGRESS, OPEN->CANCELLED, IN_PROGRESS->RESOLVED, IN_PROGRESS->CANCELLED,
RESOLVED->CLOSED; CLOSED/CANCELLED terminal; same-status PATCH is a no-op 200; else 422 with
allowed list. API: POST /api/tickets (201); GET /api/tickets?status=&q= (200 array);
GET /api/tickets/{id} (200); PUT /api/tickets/{id} fields-only (200, never status);
PATCH /api/tickets/{id}/status {"status"} (200 or 422); POST+GET /api/tickets/{id}/comments
(201/200); DELETE /api/tickets/{id} (204). Errors: 400 validation {fields}, 404 missing ticket,
422 illegal transition, bad enum -> 400. UI: list+search/filter, detail with all 5 status buttons
(backend rejects illegal ones visibly), edit form, comments, error banner, no silent failures.
Persistence: jdbc:h2:file:./data/ticketdb, ddl-auto=update, H2 console /h2-console. Tests: 9
MockMvc tests (3 valid lifecycles, 4 invalid->422, 2 validation->400). Non-goals: auth/roles,
attachments, SLA, email. End with acceptance criteria + file map. Do NOT write code yet.
```

**Expected output/files:** New `SPEC.md` (§§ requirements, domain, state machine, API, validation, UI/UX, persistence, tests, acceptance, file map). No `.java`/`.jsx` yet.

**How to verify:** Open `SPEC.md`; check F1–F11 table, transition table, and the 9-endpoint list are all present.

**What to say while demoing:** "We wrote the contract before the code — every later prompt just implements a section of this spec."

---

## Stage (b) — Backend scaffold prompts (domain → repo → service → web → tests)

**Goal:** Build the backend in smallest-reviewable slices, in dependency order.

**Exact prompt 1 — domain (paste first):**

```text
Using SPEC.md sections Domain + State machine: create backend domain only.
Files: domain/Priority.java (LOW|MEDIUM|HIGH|URGENT), domain/TicketStatus.java (5 values +
canTransitionTo() exactly OPEN->IN_PROGRESS,CANCELLED; IN_PROGRESS->RESOLVED,CANCELLED;
RESOLVED->CLOSED), domain/Ticket.java (table tickets, IDENTITY id, title 1..200, description
1..4000 LOB-safe, status STRING default OPEN, priority STRING default MEDIUM, assignee 0..100,
createdAt immutable + updatedAt Instants, OneToMany comments cascade ALL orphanRemoval),
domain/Comment.java (table comments, ManyToOne ticket nullable=false, body 1..2000, author 0..100,
createdAt). Package com.example.ticketai. Follow existing conventions; no service/web code yet.
```

**Exact prompt 2 — repositories:**

```text
Using SPEC.md API+Domain: create TicketRepository and CommentRepository (JpaRepository) only.
TicketRepository needs derived queries for exact status filter + case-insensitive partial search
on title OR description (ContainingIgnoreCase), combinable, plus Pageable variants for listPaged
(default order id DESC). CommentRepository: findByTicketId. No service/web code.
```

**Exact prompt 3 — service:**

```text
Using SPEC.md State machine + Validation: create TicketService with
create/list/listPaged/get/update/changeStatus/addComment/listComments/delete plus
NotFoundException (404) and InvalidStatusTransitionException (422).
Rules: changeStatus loads or 404; same-status returns as-is (200 no-op); illegal transition throws
with message "Invalid status transition from X to Y. Allowed: [...]"; blank assignee/author -> null;
createdAt never updated, updatedAt refreshed on mutation; PUT never changes status.
```

**Exact prompt 4 — web layer:**

```text
Using SPEC.md API contract + Validation + CORS: create web layer only.
DTOs with Bean Validation: TicketRequest, TicketUpdateRequest, StatusChangeRequest, CommentRequest,
TicketResponse (+comments), CommentResponse. TicketController with all 9 endpoints:
POST /api/tickets (201+Location), GET /api/tickets dual-mode (no page/size/sort -> bare array;
any present -> pagination envelope {content,page,size,totalElements,totalPages} with parseSort,
default id,desc), GET /{id}, PUT /{id}, PATCH /{id}/status, POST+GET /{id}/comments,
DELETE /{id} (204). GlobalExceptionHandler: validation->400 {fields}, NotFound->404,
InvalidTransition->422, IllegalArgumentException (bad enum)->400. @CrossOrigin for
http://localhost:5174, http://localhost:5173, http://localhost:3000.
Config: application.properties per spec (server.port=8080, H2 file ./data/ticketdb
AUTO_SERVER=TRUE, sa/empty, ddl-auto=update, h2-console) + test override (mem:testdb, create-drop).
```

**Exact prompt 5 — tests:**

```text
Create backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java
(@SpringBootTest + MockMvc, @Transactional, in-memory H2) with exactly 9 tests:
validLifecycle_open_to_closed, valid_open_to_cancelled, valid_inProgress_to_cancelled (2xx);
invalid_open_to_resolved_isRejected, invalid_closed_to_open_isRejected,
invalid_resolved_to_open_isRejected, invalid_cancelled_to_open_isRejected (all 422);
validation_create_requiresTitleAndDescription, comment_requiresBody (both 400).
```

**Expected output/files:** `backend/pom.xml` (Java 21, parent 3.2.5, web/jpa/validation/h2/test) + all 18 files under `backend/src/main/java/com/example/ticketai/` + both `application.properties` + `TicketStateMachineTest.java`.

**How to verify:** `cd backend && mvn -q compile` after prompts 1–4; `mvn test` after prompt 5 → 9 green.

**What to say while demoing:** "Each prompt is one reviewable slice — domain, then repos, then service, then web, then tests."

---

## Stage (c) — Frontend scaffold prompts (standalone repo)

**Goal:** Scaffold the standalone React frontend against the running backend contract.

**Exact prompt to paste (in `ticket_frontend/ticket_frontend_ai/`):**

```text
Scaffold standalone Vite+React frontend per SPEC_CONSOLIDATED.md section 10.
package.json: react/react-dom ^18.3.1, react-router-dom@6 ^6.30.6, vite ^5.4.8,
@vitejs/plugin-react ^4.3.1; scripts dev/build/preview (preview --port 5174).
vite.config.js: port 5174, proxy /api -> http://localhost:8080.
src/main.jsx (BrowserRouter), src/App.jsx (header "Support Ticket System" + meta line
"Backend: Spring Boot :8080 · Frontend: Vite :5174 · DB: H2 file", nav Tickets->/ and
+ New ticket->/create, routes / ->ListPage, /create ->CreatePage, /tickets/:id ->DetailPage,
/tickets/:id/edit ->EditPage, * ->ListPage).
src/api.js: BASE = import.meta.env.VITE_API_URL ?? '', parseError (join fields or error text),
handle (throw on non-OK, 204->null), toPage (bare array -> {content,page:0,...}),
list/get/create/update/changeStatus/remove/addComment + STATUSES, PRIORITIES, PAGE_SIZES [5,10,20],
allowedNext() per state machine.
Pages: ListPage (search input, status dropdown, Apply/Reset, table ID/Title/Status pill/Priority/
Assignee, row->/tickets/:id, pagination label "Showing from–to of total · page x of y",
size selector, Prev/numbered window width 5/Next, ErrorBanner), CreatePage (title/description*,
priority default MEDIUM, assignee blank->null, -> /tickets/:id), DetailPage (meta line, all 5
status buttons with allowed/rejected titles + "Allowed next: ..." hint, Edit link, Delete+confirm->/,
comments list + add form), EditPage (preload + PUT -> detail). styles.css + ErrorBanner everywhere,
server messages authoritative. npm run build must be clean.
```

**Expected output/files:** `package.json`, `vite.config.js`, `index.html`, `src/main.jsx`, `src/App.jsx`, `src/api.js`, `src/styles.css`, `src/pages/{ListPage,CreatePage,DetailPage,EditPage}.jsx`.

**How to verify:** `npm install && npm run dev` → UI at `http://localhost:5174`; walk `/` → `/create` → `/tickets/1` → `/tickets/1/edit`; `npm run build` clean.

**What to say while demoing:** "The frontend is a separate repo on port 5174 — same API contract, routed pages, and buttons that let the backend prove its 422s."

---

## Stage (d) — Verification prompts (tests, curl, restart persistence)

**Goal:** Prove green tests, all endpoints, and file persistence — the "it really works" moment.

**Exact prompt to paste:**

```text
Verify the build end-to-end and report pass/fail per line, stopping at first failure:
1) cd backend && mvn test (expect 9 green in TicketStateMachineTest).
2) mvn spring-boot:run, then run every curl in SPEC_CONSOLIDATED.md section 6.5 in order
(create->201, legacy list/filter/search->200, paged list->envelope 200, detail->200,
update->200, valid PATCH->200, invalid PATCH OPEN->RESOLVED->422, add+list comments->201/200,
delete->204) and show status codes.
3) Persistence: create a ticket, stop backend, restart, GET /api/tickets must still list it
(H2 file backend/data/ticketdb.mv.db). 4) Frontend: npm run build clean + manual walkthrough
in section 14 (lifecycle to CLOSED, 422 on CLOSED->OPEN, search/filter/pagination, delete->404).
```

**Expected output/files:** Terminal output (9 tests green, curl `-i` status lines, `backend/data/ticketdb.mv.db` present); no new source files.

**How to verify:** Run yourself:

```bash
cd backend && mvn test
curl -i -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Login broken","description":"500 on submit","priority":"HIGH","assignee":"ada"}'
curl 'localhost:8080/api/tickets?page=0&size=5'
curl -i -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' -d '{"status":"RESOLVED"}'
# expect 422 on a fresh OPEN ticket; restart backend; curl 'localhost:8080/api/tickets' still shows data
```

**What to say while demoing:** "Green tests, curl status codes, and a backend restart — persistence is the crowd-pleaser."

---

## Stage (e) — PR-split prompts (5 reviewable slices)

**Goal:** Split the work into small reviewable PRs instead of one big diff.

**Exact prompt to paste:**

```text
Split this work into 5 stacked reviewable slices (entities -> repos -> service -> web -> frontend),
each independently explainable. For each slice list files + why (not what), keep diffs minimal,
no unrelated cleanup. Branch naming cursor/<ticket>-<summary>, never push to main.
Slice 1 domain entities, 2 repositories, 3 service+exceptions, 4 controller+DTOs+handler+config,
5 frontend pages+api. Also snapshot current work on branch backup/main-work-20260926.
```

**Expected output/files:** 5 commit/PR descriptions + branch `backup/main-work-20260926`; no code changes.

**How to verify:** `git branch --list 'backup/*'` and `git log --oneline -5` show the snapshot; each slice's file list matches the inventory table.

**What to say while demoing:** "Five small PRs beat one giant one — each slice maps to one spec section."

---

## Stage (f) — Postman collection prompt

**Goal:** Turn the verified curls into a shareable Postman collection.

**Exact prompt to paste:**

```text
From SPEC_CONSOLIDATED.md section 6 (endpoint table + curls 1-9 incl. 6b invalid->422),
generate postman/TicketAI.postman_collection.json (collection v2.1, baseUrl http://localhost:8080):
create 201, legacy list + status/q combos, paged list, detail, update PUT, valid PATCH,
invalid PATCH (expect 422), add comment 201, list comments, delete 204. JSON bodies exactly per
section 6.4 shapes. Do not modify SPEC.md, README.md, or source files.
```

**Expected output/files:** `postman/TicketAI.postman_collection.json` only.

**How to verify:** Import into Postman → run in order against `localhost:8080`; invalid-PATCH request returns 422.

**What to say while demoing:** "Same curls you just saw, now one-click runnable for anyone."

---

## Stage (g) — Repo-split prompt (frontend goes standalone)

**Goal:** Explain and execute freezing the legacy `frontend/` copy and moving active UI work to the standalone repo.

**Exact prompt to paste:**

```text
Split the repos: backend stays in ticket-ai; frontend becomes standalone at
ticket_frontend/ticket_frontend_ai. Freeze any legacy frontend/ copy inside ticket-ai
(do not extend it). New standalone repo gets package.json (port 5174), vite.config.js
(/api proxy -> :8080), src/{main.jsx,App.jsx,api.js,styles.css,pages/*}. Backend CORS must allow
http://localhost:5174 (+5173,3000 legacy). Verify: backend :8080 + frontend :5174 run together,
relative /api calls work via proxy.
```

**Expected output/files:** Standalone `ticket_frontend/ticket_frontend_ai/` tree; backend `@CrossOrigin` updated; legacy `frontend/` untouched.

**How to verify:** Both servers up; `http://localhost:5174/` loads data (proxy, no CORS errors in devtools).

**What to say while demoing:** "One repo per deployable — the old embedded copy is frozen history."

---

## Stage (h) — Pages + pagination prompt (router + envelope)

**Goal:** Add routed pages and the pagination envelope — the two biggest post-v1 upgrades.

**Exact prompt to paste:**

```text
Upgrade per SPEC_CONSOLIDATED.md sections 6.2 + 10.2 + 10.4. Backend: dual-mode
GET /api/tickets — no page/size/sort -> bare array (legacy); any present -> envelope
{content,page,size,totalElements,totalPages}, params status,q,page(0-based,default 0),
size(default 10), repeatable sort (default id,desc) via parseSort. Frontend: react-router-dom@6
routes / (ListPage), /create, /tickets/:id, /tickets/:id/edit (+ * fallback); ListPage pagination:
label "Showing {from}–{to} of {total} · page {n} of {m}", PAGE_SIZES [5,10,20] resetting to page 0,
Prev/numbered window width 5/Next with clamping, toPage() normalizing bare arrays; DetailPage keeps
all 5 status buttons + allowedNext() hint so 422s are demonstrable. Acceptance: bare list still an
array; paged list is the envelope; counts/size/Prev/numbers/Next all work.
```

**Expected output/files:** `TicketController.java` (dual-mode + `parseSort`), `TicketService.java` (`listPaged`), `src/api.js` (`toPage`), `src/App.jsx` (routes), `src/pages/ListPage.jsx` (pagination), `src/pages/DetailPage.jsx` (buttons+hint).

**How to verify:** `curl 'localhost:8080/api/tickets'` → `[...]`; `curl 'localhost:8080/api/tickets?page=0&size=5'` → `{content,page,size,totalElements,totalPages}`; UI paginates 5/10/20 with counts.

**What to say while demoing:** "Legacy callers still get an array — the UI opts into the envelope just by passing page or size."

---

## Stage (i) — Consolidated-spec prompt (this single-file contract)

**Goal:** Produce the one file that can rebuild everything from scratch.

**Exact prompt to paste:**

```text
Write docs/SPEC_CONSOLIDATED.md as the single self-contained build contract that supersedes
SPEC.md/README/BACKEND/FRONTEND/CHAT_HISTORY as a build source (do NOT modify SPEC.md).
It must contain all 14 sections: goal+F1-F13, stack+ports (Java 21, Boot 3.2.5, Vite 5, React 18,
router 6, :8080/:5174, H2 file vs mem-test), both repo file maps, domain model, state-machine
diagram+table+422 rules, full API contract (9 endpoints, pagination params+envelope, error
envelopes, shapes, all curls), validation table, persistence config (both properties files),
CORS, frontend spec (config/routes/api.js/pages/error handling), 9-test list, acceptance
checklist, step-by-step build order (backend 1-7, frontend 8-11), verification commands.
An agent receiving ONLY this file must rebuild the system completely.
```

**Expected output/files:** `docs/SPEC_CONSOLIDATED.md` only (≈600 lines, §§1–14).

**How to verify:** Skim §§1/5/6/10/13 — F1–F13, transition table, 9-endpoint table with envelope, routes table, and build order all present; `git status` shows no modification to `SPEC.md`.

**What to say while demoing:** "Hand this one file to any agent and it rebuilds the whole system — that's the finale."

---

## Demo run-of-show (suggested order + timing)

1. Inventory table (1 min) — show what exists.
2. Stage (a) spec-first (3 min) — the "why it worked" story.
3. Stage (b) backend slices (5 min) — scroll domain→service→controller quickly.
4. Stage (d) verification live (5 min) — `mvn test` + 2–3 curls + restart. Do this early for wow-factor if short on time.
5. Stages (c)+(g)+(h) frontend story (5 min) — standalone repo, routes, pagination in browser.
6. Stages (e)+(f)+(i) process wins (3 min) — PR split, Postman, consolidated spec close.

Tip: keep two terminals open (backend `:8080`, frontend `:5174`) + Postman + H2 console (`/h2-console`, `jdbc:h2:file:./data/ticketdb`, `sa`/empty) before starting.
