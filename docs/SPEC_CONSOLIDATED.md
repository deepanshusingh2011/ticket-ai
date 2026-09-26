# SPEC_CONSOLIDATED.md — Support Ticket Management System (Self-Contained Build Spec)

> **How to use this file:** hand ONLY this file to an agent and it can rebuild the entire
> system (backend + frontend) from scratch, all features at once. No other file is required.
>
> **History note:** v1 design lived in `SPEC.md` (plus `README.md`, `docs/BACKEND.md`,
> `docs/FRONTEND.md`, `docs/CHAT_HISTORY.md`). This file supersedes them as the single
> build contract but does not depend on them. Do not modify `SPEC.md`; treat it as v1 history.

## Contents

1. [Goal & scope](#1-goal--scope)
2. [Stack & ports](#2-stack--ports)
3. [Repos & file maps](#3-repos--file-maps)
4. [Domain model](#4-domain-model)
5. [State machine](#5-state-machine-backend-enforced)
6. [API contract](#6-api-contract)
7. [Validation rules](#7-validation-rules)
8. [Persistence & config](#8-persistence--config)
9. [CORS](#9-cors)
10. [Frontend spec](#10-frontend-spec)
11. [Tests](#11-tests-9-state-machine-tests)
12. [Acceptance criteria](#12-acceptance-criteria)
13. [Build order](#13-step-by-step-build-order)
14. [Verification](#14-verification)

---

## 1. Goal & scope

Minimal support-ticket tracker for small co-located teams: create tickets, triage through a
controlled lifecycle, edit fields, add discussion comments, search by keyword, filter by status,
paginate the list. Single-team local tool.

**Functional requirements:**

| ID | Requirement |
|----|-------------|
| F1 | Create ticket (title, description, priority, assignee) |
| F2 | List tickets (paginated) |
| F3 | View ticket details incl. comments |
| F4 | Update title / description / priority / assignee |
| F5 | Change status only via allowed transitions (state machine, backend-enforced) |
| F6 | Add comments to a ticket |
| F7 | Search by keyword (title + description, case-insensitive, partial match) |
| F8 | Filter by status |
| F9 | Combine status filter + keyword search |
| F10 | Persist data across restarts (H2 file mode) |
| F11 | Backend validation with meaningful UI errors |
| F12 | Delete ticket |
| F13 | Paginated list with page/size controls, page-size selector, Prev/Next + numbered buttons, result counts |

**Explicit non-goals (v1):** authentication/authorization, roles, file attachments, SLA timers,
email notifications.

---

## 2. Stack & ports

| Layer | Technology (pin these) |
|-------|------------------------|
| Backend | Java 21, Spring Boot 3.2.x (parent `3.2.5`), Spring Web + Data JPA + Validation, H2 (runtime), spring-boot-starter-test |
| Backend build | Maven (`mvn`), `backend/pom.xml` with `<java.version>21</java.version>` |
| Backend port | `8080` (`server.port=8080`) |
| Frontend | Vite 5 (`^5.4.8`), React 18 (`^18.3.1`), `react-dom` 18, `react-router-dom@6` (`^6.30.6`), `@vitejs/plugin-react` 4 |
| Frontend build | npm (`npm install`, `npm run dev`, `npm run build`) |
| Frontend port | `5174` (dev server + preview) |
| DB | H2 file mode (dev) / H2 in-memory (tests) |
| Data format | JSON everywhere; UTF-8 |

Prerequisites: Java 21 (`java -version`), Maven 3.6+ (`mvn -v`), Node 18+ (`node -v`).

---

## 3. Repos & file maps

Two repos/siblings. Backend lives in the main repo; the frontend is a **standalone repo**.
(The legacy `frontend/` copy inside the backend repo, if present, is frozen — do not extend it.)

**Repo A — backend** (`ticket-ai`, branch e.g. `backup/main-work-20260926`):

```
backend/pom.xml
backend/src/main/java/com/example/ticketai/
  TicketAiApplication.java
  domain/Ticket.java
  domain/TicketStatus.java        # enum + canTransitionTo()
  domain/Priority.java            # LOW | MEDIUM | HIGH | URGENT
  domain/Comment.java
  repository/TicketRepository.java
  repository/CommentRepository.java
  service/TicketService.java
  service/NotFoundException.java
  service/InvalidStatusTransitionException.java
  web/TicketController.java
  web/GlobalExceptionHandler.java
  web/dto/TicketRequest.java
  web/dto/TicketUpdateRequest.java
  web/dto/StatusChangeRequest.java
  web/dto/CommentRequest.java
  web/dto/TicketResponse.java
  web/dto/CommentResponse.java
backend/src/main/resources/application.properties
backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java
backend/src/test/resources/application.properties   # in-memory H2 for tests
docs/SPEC_CONSOLIDATED.md   # this file (new; SPEC.md untouched)
postman/TicketAI.postman_collection.json  # optional, regenerate from §6 curls
```

**Repo B — frontend standalone** (`ticket_frontend/ticket_frontend_ai`):

```
package.json            # name ticket-ai-frontend, scripts dev/build/preview, deps above
vite.config.js          # port 5174, proxy /api -> http://localhost:8080
index.html
src/main.jsx            # React root + <BrowserRouter>
src/App.jsx             # header/nav + <Routes>
src/api.js              # fetch wrapper + STATUSES/PRIORITIES/PAGE_SIZES/allowedNext()
src/styles.css
src/pages/ListPage.jsx      # list + filters + pagination + ErrorBanner
src/pages/CreatePage.jsx    # create form
src/pages/DetailPage.jsx    # detail + status buttons + comments + delete
src/pages/EditPage.jsx      # edit form (PUT)
```

---

## 4. Domain model

```
Ticket (table: tickets)
  id:          Long, PK, auto-increment (GenerationType.IDENTITY)
  title:       String, required, 1..200
  description: String, required, 1..4000 (TEXT/LOB-safe length)
  status:      TicketStatus enum (STRING mapping), required, default OPEN
  priority:    Priority enum (STRING mapping), required, default MEDIUM
  assignee:    String | null, 0..100; blank/whitespace-only -> null on write
  createdAt:   Instant, immutable (set on create, never updated)
  updatedAt:   Instant, set on create, refreshed on every mutation
  comments:    List<Comment>, @OneToMany(mappedBy ticket, cascade ALL, orphanRemoval true)

Comment (table: comments)
  id:        Long, PK, auto-increment
  ticket:    ManyToOne -> Ticket, FK ticket_id, required (nullable=false)
  body:      String, required, 1..2000
  author:    String | null, 0..100; blank -> null
  createdAt: Instant, immutable

TicketStatus: OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED
Priority:     LOW | MEDIUM | HIGH | URGENT
```

Implementation notes:

- `TicketStatus.canTransitionTo(TicketStatus target)` returns true exactly for:
  `OPEN→IN_PROGRESS`, `OPEN→CANCELLED`, `IN_PROGRESS→RESOLVED`, `IN_PROGRESS→CANCELLED`,
  `RESOLVED→CLOSED`. Same-status is handled by the service as a no-op (returns current ticket).
- `TicketService.changeStatus(id, target)`: load or 404; if `current == target` return as-is;
  else if `!current.canTransitionTo(target)` throw `InvalidStatusTransitionException`
  (→ HTTP 422) with message naming the transition and allowed targets.
- Search: keyword `q` matches `title` OR `description`, case-insensitive, partial
  (`LIKE %q%`). Status filter is exact enum match. Both combinable; both optional.
- List ordering default: `id DESC` (newest first). Sort param (paged mode) overrides.
- `TicketResponse` includes: `id, title, description, status, priority, assignee,
  createdAt, updatedAt, comments[]` (each: `id, body, author, createdAt`).
- Timestamps serialized as ISO-8601 instants.

---

## 5. State machine (backend-enforced)

```
OPEN ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
 │            │
 └─→ CANCELLED └─→ CANCELLED
```

| From | Allowed next |
|------|--------------|
| OPEN | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | RESOLVED, CANCELLED |
| RESOLVED | CLOSED |
| CLOSED | — (terminal) |
| CANCELLED | — (terminal) |

Rules:

- Enforced in `TicketStatus.canTransitionTo()` + `TicketService.changeStatus()`.
- Same-status PATCH is a no-op → `200` with current ticket (not an error).
- Every other transition → `InvalidStatusTransitionException` → **HTTP 422** with body
  `{"timestamp","status":422,"error":"Invalid status transition from X to Y. Allowed: [...]"}`
- `CLOSED` and `CANCELLED` are terminal: any change attempt (to a different status) → 422.
- Unknown enum value in the status body → 400 (see §7), not 422.
- Frontend MUST render all five statuses as clickable buttons so reviewers can observe the
  backend 422 rejection, plus an `allowedNext()` hint line.

---

## 6. API contract

Base URL: `http://localhost:8080`. All requests/responses JSON. `Content-Type: application/json`
on all bodies.

### 6.1 Endpoint table

| # | Method | Path | Success | Description |
|---|--------|------|---------|-------------|
| 1 | POST | `/api/tickets` | 201 + ticket | Create ticket |
| 2 | GET | `/api/tickets?status=&q=` | 200 bare array | List / filter / search (legacy, no page/size → full list) |
| 3 | GET | `/api/tickets?status=&q=&page=&size=&sort=` | 200 pagination envelope | Paginated list (used by the UI) |
| 4 | GET | `/api/tickets/{id}` | 200 ticket + comments | Details |
| 5 | PUT | `/api/tickets/{id}` | 200 ticket | Update title/description/priority/assignee (status NOT updatable here) |
| 6 | PATCH | `/api/tickets/{id}/status` | 200 ticket, or 422 | Change status, body `{"status":"IN_PROGRESS"}` |
| 7 | POST | `/api/tickets/{id}/comments` | 201 comment | Add comment |
| 8 | GET | `/api/tickets/{id}/comments` | 200 comment array | List comments |
| 9 | DELETE | `/api/tickets/{id}` | 204 empty | Delete ticket |

### 6.2 Pagination params & envelope (endpoint #3)

Request params:

- `status` (optional enum): exact match filter.
- `q` (optional string, param name is `q`): keyword search, case-insensitive partial on
  title+description.
- `page` (optional int, 0-based, default `0`).
- `size` (optional int, default `10`).
- `sort` (optional, repeatable `sort=property,dir`, e.g. `sort=id,desc`): Spring `Sort`
  syntax; default `id,desc` when absent/blank.

Mode rule (controller): if **none** of `page/size/sort` are present → return bare JSON array
(legacy behavior). If **any** is present → return the envelope below.

Envelope (`200`):

```json
{
  "content": [ { "id": 1, "title": "…", "status": "OPEN", "priority": "HIGH", "assignee": "ada", "createdAt": "…", "updatedAt": "…", "description": "…", "comments": [] } ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

### 6.3 Error envelope

- `400` validation: `{"timestamp":"…","status":400,"error":"Validation failed","fields":{"title":"…"}}`
- `404` missing ticket: `{"timestamp":"…","status":404,"error":"Ticket 99 not found"}`
- `422` illegal transition: `{"timestamp":"…","status":422,"error":"Invalid status transition from OPEN to RESOLVED. Allowed: [IN_PROGRESS, CANCELLED]"}`
- Unknown enum value (e.g. `"priority":"P9"`) → `400` via `IllegalArgumentException` handler.

### 6.4 Shapes

Create (`POST /api/tickets`) body — `priority`/`assignee` optional:

```json
{ "title": "Login broken", "description": "500 on submit", "priority": "HIGH", "assignee": "ada" }
```

Update (`PUT /api/tickets/{id}`) body — all four writable fields:

```json
{ "title": "Login broken (upd)", "description": "500 on submit", "priority": "URGENT", "assignee": "ada" }
```

Status change (`PATCH /api/tickets/{id}/status`) body:

```json
{ "status": "IN_PROGRESS" }
```

Add comment (`POST /api/tickets/{id}/comments`) body:

```json
{ "body": "Repro confirmed", "author": "ada" }
```

### 6.5 curl per endpoint (run against local backend)

```bash
# 1. Create -> 201
curl -i -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Login broken","description":"500 on submit","priority":"HIGH","assignee":"ada"}'

# 2. List (legacy bare array) + filter/search combos -> 200
curl 'localhost:8080/api/tickets'
curl 'localhost:8080/api/tickets?status=OPEN'
curl 'localhost:8080/api/tickets?q=login'
curl 'localhost:8080/api/tickets?status=OPEN&q=login'

# 3. Paginated list (envelope) -> 200
curl 'localhost:8080/api/tickets?page=0&size=5'
curl 'localhost:8080/api/tickets?status=OPEN&q=login&page=0&size=10&sort=id,desc'

# 4. Details -> 200 (404 if missing)
curl localhost:8080/api/tickets/1

# 5. Update fields -> 200
curl -X PUT localhost:8080/api/tickets/1 -H 'Content-Type: application/json' \
  -d '{"title":"Login broken (upd)","description":"500 on submit","priority":"URGENT","assignee":"ada"}'

# 6. Status change (valid) -> 200
curl -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'

# 6b. Status change (invalid, e.g. OPEN -> RESOLVED) -> 422
curl -i -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' \
  -d '{"status":"RESOLVED"}'

# 7. Add comment -> 201
curl -X POST localhost:8080/api/tickets/1/comments -H 'Content-Type: application/json' \
  -d '{"body":"Repro confirmed","author":"ada"}'

# 8. List comments -> 200
curl localhost:8080/api/tickets/1/comments

# 9. Delete -> 204
curl -i -X DELETE localhost:8080/api/tickets/1
```

Expected status codes: create 201 (+ `Location: /api/tickets/{id}`), paged/legacy list 200,
details 200, update 200, status-change 200 or 422 (400 on bad enum), add-comment 201
(+ `Location: /api/tickets/{id}/comments/{commentId}`), list-comments 200, delete 204.

---

## 7. Validation rules

| Field | Rule | Failure |
|-------|------|---------|
| `ticket.title` | required, non-blank, 1..200 | 400 `fields.title` |
| `ticket.description` | required, non-blank, 1..4000 | 400 `fields.description` |
| `ticket.priority` | optional enum, default MEDIUM; unknown value rejected | 400 on unknown value |
| `ticket.assignee` | optional, ≤100 chars; blank/whitespace → stored null | 400 `fields.assignee` |
| status body `.status` | required enum | 400 if missing/unknown; 422 if legal enum but illegal transition |
| `comment.body` | required, non-blank, 1..2000 | 400 `fields.body` |
| `comment.author` | optional, ≤100 chars; blank → null | 400 `fields.author` |
| path `id` | must exist | 404 `Ticket {id} not found` |

Backend uses Bean Validation (`@Valid` on `@RequestBody` DTOs) + `GlobalExceptionHandler`
mapping: validation → 400+`fields`, `NotFoundException` → 404, `InvalidStatusTransitionException`
→ 422, `IllegalArgumentException` (bad enum) → 400. PUT must NOT change status — status changes
only via PATCH.

---

## 8. Persistence & config

`backend/src/main/resources/application.properties` (dev source of truth):

```properties
server.port=8080

# H2 file-based persistence so data survives restarts (stored under ./data/
# relative to the backend/ working directory)
spring.datasource.url=jdbc:h2:file:./data/ticketdb;AUTO_SERVER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- Data file lands in `backend/data/` (`ticketdb.mv.db`). `ddl-auto=update`; data survives
  backend restarts. H2 console at `http://localhost:8080/h2-console`
  (JDBC URL `jdbc:h2:file:./data/ticketdb`, user `sa`, empty password — dev only, never commit secrets).
- Tests MUST NOT touch the file DB: `backend/src/test/resources/application.properties`
  overrides with in-memory H2 (e.g. `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`, same `sa`/empty,
  `ddl-auto=create-drop`).

---

## 9. CORS

- Backend allows (per-controller `@CrossOrigin` on `TicketController`):
  `http://localhost:5174` (standalone frontend), `http://localhost:5173` (legacy),
  `http://localhost:3000`.
- Frontend dev server additionally proxies `/api → http://localhost:8080` (see `vite.config.js`),
  so relative `/api/...` fetch calls work without CORS in dev.

---

## 10. Frontend spec

### 10.1 Stack / config

- `package.json`: `react ^18.3.1`, `react-dom ^18.3.1`, `react-router-dom ^6.30.6`;
  dev: `vite ^5.4.8`, `@vitejs/plugin-react ^4.3.1`; scripts `dev: vite`,
  `build: vite build`, `preview: vite preview --port 5174`.
- `vite.config.js`: `plugins: [react()]`, `server: { port: 5174, proxy: { '/api': 'http://localhost:8080' } }`.
- `src/main.jsx`: `createRoot(...).render(<React.StrictMode><BrowserRouter><App/></BrowserRouter></React.StrictMode>)`.

### 10.2 Routes (react-router-dom@6, in `src/App.jsx`)

| Route | Page | Purpose |
|-------|------|---------|
| `/` | `ListPage` | Ticket table + search/filter + pagination |
| `/create` | `CreatePage` | Create form → POST → navigate to `/tickets/{id}` |
| `/tickets/:id` | `DetailPage` | Detail + status buttons + comments + delete |
| `/tickets/:id/edit` | `EditPage` | Edit form (PUT) → navigate back to detail |
| `*` | → `ListPage` | Fallback |

Header/nav on every page: title `Support Ticket System`, meta line
`Backend: Spring Boot :8080 · Frontend: Vite :5174 · DB: H2 file`, nav buttons
`Tickets → /`, `+ New ticket → /create`.

### 10.3 API client (`src/api.js`)

- `BASE = import.meta.env.VITE_API_URL ?? ''` (relative URLs through Vite proxy by default).
- `parseError(res)`: if body has `fields`, join as `k: v; …`; else `error` text; fallback
  `Request failed ({status})`. `handle(res)`: throw `Error(parsed)` on non-OK; `204 → null`.
- `toPage(data)`: paged envelope returned as-is; **bare array** wrapped to
  `{content, page:0, size:length, totalElements:length, totalPages:1}` so list UI treats both shapes uniformly.
- Exports:
  - `list({status, q, page, size, sort})` → `GET /api/tickets?...` → normalized page object.
  - `get(id)` → `GET /api/tickets/{id}`.
  - `create(payload)` → `POST /api/tickets`.
  - `update(id, payload)` → `PUT /api/tickets/{id}`.
  - `changeStatus(id, status)` → `PATCH /api/tickets/{id}/status`.
  - `remove(id)` → `DELETE /api/tickets/{id}` → null.
  - `addComment(id, payload)` → `POST /api/tickets/{id}/comments`.
  - `STATUSES = ['OPEN','IN_PROGRESS','RESOLVED','CLOSED','CANCELLED']`,
    `PRIORITIES = ['LOW','MEDIUM','HIGH','URGENT']`, `PAGE_SIZES = [5,10,20]`,
    `allowedNext(status)` helper (OPEN→IN_PROGRESS/CANCELLED, IN_PROGRESS→RESOLVED/CANCELLED,
    RESOLVED→CLOSED, else []).

### 10.4 Pages & pagination UX

**ListPage (`/`)** — state: `tickets, status, query, page (0-based), size (default 10),
totalElements, totalPages, error, loading`.

- On mount: `refresh(0, size, '', '')`.
- `refresh(pageArg, sizeArg, statusArg, qArg)` calls `api.list(...)` with
  `status||undefined, q||undefined, page, size`; sets `tickets = data.content || []` plus page metadata.
- Filter bar: Search input (`placeholder "keyword in title/description"`), Status dropdown
  (`All` + five statuses), `Apply` (resets to page 0, refetches), `Reset` (clears both, page 0),
  `+ New ticket` link.
- Table columns: ID, Title, Status (pill), Priority, Assignee (`—` when null). Row click →
  `navigate(/tickets/{id})`. Empty state: `No tickets yet. Create one.` Loading state: `Loading…`.
- Pagination controls (always rendered with the table):
  - Label: `Showing {from}–{to} of {totalElements} · page {page+1} of {totalPages}`
    (`from = totalElements===0 ? 0 : page*size+1`, `to = min(total, page*size+tickets.length)`).
  - Page-size selector (`5/10/20 per page`) → resets to page 0.
  - `← Prev` / numbered window (`pageWindow(current,total,width=5)`, 1-based labels, current
    disabled) / `Next →` (Prev disabled at first page; Next disabled at last/empty). Out-of-range
    `goTo()` clamps.

**CreatePage (`/create`)** — controlled form: `title*` (`required`, `maxLength 200`),
`description*` (`required`, `maxLength 4000`, textarea), `priority` select (default MEDIUM),
`assignee` input (`maxLength 100`, blank → send null/omit). Submit → `api.create` → navigate to
`/tickets/{id}`; failure → error banner. Cancel/back link to `/`.

**DetailPage (`/tickets/:id`)** — loads via `api.get(id)` with `loading` state; `Ticket not found.`
+ back link when null.

- Meta line: `Status: X · Priority: Y · Assignee: Z`.
- Status buttons: all five `STATUSES` rendered; current disabled; others clickable with
  `title = allowed ? 'Allowed transition' : 'Will be rejected by backend'`; click →
  `api.changeStatus` → refresh ticket or show 422 in banner.
- Hint: `Allowed next: {allowedNext(status).join(', ') || 'none (terminal state)'}. Other buttons demonstrate backend rejection.`
- `Edit → /tickets/{id}/edit`; `Delete` → `confirm(...)` → `api.remove` → `navigate('/')`.
- Comments: `Comments (n)` list (`author || 'anonymous'`, locale date, body) + add form
  (`body*` textarea `maxLength 2000 required`, `author` input `maxLength 100 optional`) → POST →
  clear + reload ticket.

**EditPage (`/tickets/:id/edit`)** — preloads via `api.get(id)`; same field rules as create;
submit → `api.update(id, {title, description, priority, assignee})` → navigate to `/tickets/{id}`.

### 10.5 Error handling (all pages)

- Shared `ErrorBanner` (defined in `ListPage.jsx`, reused by Detail): top of page, shows
  `err.message` (already-joined 400 `fields` text or raw 422 text), dismissible `×` button,
  renders nothing when empty. No silent failures — every fetch failure sets the banner.
- Client mirrors backend with `required`/`maxLength` attributes but ALWAYS surfaces server
  messages (server is authoritative).

---

## 11. Tests (9 state-machine tests)

File: `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java`
(`@SpringBootTest` + MockMvc, `@Transactional`, in-memory H2). Exactly 9 tests:

Valid (expect 2xx):

1. `validLifecycle_open_to_closed` — `OPEN→IN_PROGRESS→RESOLVED→CLOSED` full walk succeeds.
2. `valid_open_to_cancelled` — `OPEN→CANCELLED` succeeds.
3. `valid_inProgress_to_cancelled` — create, move to `IN_PROGRESS`, then `→CANCELLED` succeeds.

Invalid (expect **422**):

4. `invalid_open_to_resolved_isRejected` — `OPEN→RESOLVED` rejected.
5. `invalid_closed_to_open_isRejected` — terminal `CLOSED→OPEN` rejected.
6. `invalid_resolved_to_open_isRejected` — `RESOLVED→OPEN` rejected.
7. `invalid_cancelled_to_open_isRejected` — terminal `CANCELLED→OPEN` rejected.

Validation (expect **400**):

8. `validation_create_requiresTitleAndDescription` — empty title/description rejected.
9. `comment_requiresBody` — empty comment body rejected.

Run: `cd backend && mvn test` → 9 green.

---

## 12. Acceptance criteria

- [ ] F1–F13 all work from the UI (create, paginated list, detail+comments, edit, guarded
  status moves, comments, search, status filter, combined filter+search, persistence, validation
  errors, delete, pagination controls).
- [ ] Valid transitions succeed end-to-end; invalid ones are rejected by the backend with a
  visible 422 banner (all five status buttons remain clickable by design).
- [ ] Pagination: page/size/sort params return the §6.2 envelope; bare list (no params) still
  returns an array; UI shows counts, page-size selector, Prev/numbered/Next.
- [ ] Data survives backend restart (H2 file in `backend/data/`).
- [ ] Validation errors shown meaningfully (400 `fields` joined in banner); no silent failures.
- [ ] `mvn test` green (9 tests); `npm run build` clean.
- [ ] Ports exactly `8080` (API) / `5174` (UI); CORS allows `5174/5173/3000`.
- [ ] No secrets committed; H2 uses local `sa`/empty password (dev only).
- [ ] Routes `/`, `/create`, `/tickets/:id`, `/tickets/:id/edit` all resolve (fallback `*` → list).

---

## 13. Step-by-step build order

Follow in order; each step is independently verifiable. Backend first, then frontend.

**Backend (repo A, `backend/`):**

1. **Scaffold** — Maven project from `pom.xml` (§2): `spring-boot-starter-web`,
   `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2` (runtime),
   `spring-boot-starter-test` (test scope); `java.version=21`; parent `3.2.5`.
   Verify: `cd backend && mvn -q compile` succeeds.
2. **Config** — write `application.properties` exactly per §8; test override per §8.
   Verify: `mvn spring-boot:run` starts on `:8080`, `/h2-console` reachable.
3. **Domain** — `Priority`, `TicketStatus` (+ `canTransitionTo()` per §5 table),
   `Ticket`, `Comment` entities per §4 (JPA mappings, defaults OPEN/MEDIUM, blank→null
   handled in service/setters, Instant timestamps).
4. **Repositories** — `TicketRepository`, `CommentRepository` (JpaRepository) + derived
   query for status filter + case-insensitive title/description search
   (e.g. `findByStatusAndTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase…`,
   plus pageable variants for `listPaged`).
5. **Service** — `TicketService` (`create/list/listPaged/get/update/changeStatus/addComment/
   listComments/delete`) + `NotFoundException` + `InvalidStatusTransitionException`;
   enforce §5 (same-status no-op; else 422 with allowed list) and §7 normalization.
6. **Web** — DTOs (`TicketRequest/TicketUpdateRequest/StatusChangeRequest/CommentRequest/
   TicketResponse/CommentResponse` with Bean Validation annotations per §7),
   `TicketController` (9 endpoints per §6 incl. dual-mode list + `parseSort`, `@CrossOrigin`
   per §9), `GlobalExceptionHandler` (400/404/422 + bad-enum→400 per §6.3).
7. **Tests** — `TicketStateMachineTest` with the 9 tests in §11.
   Verify: `cd backend && mvn test` → 9 green.

**Frontend (repo B, `ticket_frontend/ticket_frontend_ai/`):**

8. **Scaffold** — `package.json` + `vite.config.js` + `index.html` per §10.1
   (port 5174, `/api` proxy to `:8080`). Verify: `npm install && npm run dev` serves `:5174`.
9. **API layer** — `src/api.js` per §10.3 (`BASE`, `parseError/handle/toPage`, 7 functions +
   constants + `allowedNext`). Verify: point at running backend, list/create via console.
10. **Router shell** — `src/main.jsx` (BrowserRouter) + `src/App.jsx` (header/nav + 4 routes +
    fallback) per §10.2. Verify: all routes render without data.
11. **Pages** — `ListPage` (filters + table + §10.4 pagination), `CreatePage`, `DetailPage`
    (status buttons + comments + delete), `EditPage`, shared `ErrorBanner`, `styles.css`.
    Verify: `npm run build` clean + manual walkthrough (§14).

---

## 14. Verification

```bash
# Backend tests (9 green)
cd backend
mvn test

# Backend run
mvn spring-boot:run
# API at http://localhost:8080, H2 console at http://localhost:8080/h2-console

# Frontend run (backend must be up first)
cd ticket_frontend/ticket_frontend_ai
npm install
npm run dev      # UI at http://localhost:5174
npm run build    # must be clean
```

Manual walkthrough (must all pass):

1. Create ticket → appears in list → open `/tickets/{id}` → edit fields → add comments.
2. Walk `OPEN→IN_PROGRESS→RESOLVED→CLOSED` (all succeed); attempt `CLOSED→OPEN` (422 banner).
   Try `OPEN→RESOLVED` on a fresh ticket (422 banner).
3. Search keyword + status filter, combined, then Reset; paginate (change page size 5/10/20,
   Prev/numbered/Next, counts update).
4. Restart backend → tickets persist (H2 file in `backend/data/`).
5. Delete a ticket → 204 → redirects to `/`; direct GET on it → 404 banner/`Ticket not found`.
6. Debug aids: browser devtools network tab for 400/422 bodies; H2 console
   (`jdbc:h2:file:./data/ticketdb`, user `sa`, empty password).
