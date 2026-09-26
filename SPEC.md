# SPEC.md — Support Ticket Management System

> Purpose: teach **spec-driven development** with AI assistants (Cursor / Copilot / Kiro).
> Workflow: write this spec first → generate scaffolding from it → implement → verify against acceptance criteria.
> Stack: Java 21, Spring Boot 3.2.x, H2 (file-based), REST API, Vite + React 18 frontend.

## 1. Requirements analysis

### 1.1 Problem statement
Small support teams need a minimal ticket tracker: create tickets, triage them through a
controlled lifecycle, add discussion comments, and find tickets by keyword/status.

### 1.2 Users
- **Requester / customer**: creates tickets, adds comments.
- **Agent**: triages, updates fields, moves status, comments.
- No authentication in v1 (single-team local tool). Auth is an explicit non-goal.

### 1.3 Functional requirements
| ID | Requirement |
|----|-------------|
| F1 | Create ticket (title, description, priority, assignee) |
| F2 | List tickets |
| F3 | View ticket details incl. comments |
| F4 | Update title / description / priority / assignee |
| F5 | Change status only via allowed transitions (state machine, backend-enforced) |
| F6 | Add comments to a ticket |
| F7 | Search by keyword (title + description, case-insensitive, partial match) |
| F8 | Filter by status |
| F9 | Combine status filter + keyword search |
| F10 | Persist data across restarts (H2 file mode) |
| F11 | Backend validation with meaningful UI errors |

### 1.4 Non-functional requirements
- Backend builds with `mvn`, runs on port 8080.
- Frontend builds with `npm`, runs on port 5173.
- No secrets committed; H2 uses default local `sa` with empty password (dev only).
- CORS restricted to `http://localhost:5173` and `http://localhost:3000`.
- Code follows existing conventions; minimal, reviewable changes.

### 1.5 Out of scope (v1)
AuthN/AuthZ, file attachments, SLA timers, email notifications, pagination, roles.

## 2. Domain model

```
Ticket (tickets table)
  id: Long (PK, auto-increment)
  title: String (required, 1..200)
  description: String (required, 1..4000)
  status: TicketStatus (required, default OPEN)
  priority: Priority (required, default MEDIUM)
  assignee: String | null (0..100)
  createdAt: Instant (immutable)
  updatedAt: Instant
  comments: List<Comment> (1:N, cascade ALL, orphanRemoval)

Comment (comments table)
  id: Long (PK)
  ticket_id: FK -> tickets.id (required)
  body: String (required, 1..2000)
  author: String | null (0..100)
  createdAt: Instant (immutable)

TicketStatus: OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED
Priority: LOW | MEDIUM | HIGH | URGENT
```

## 3. State machine (backend-enforced)

Allowed transitions:

```
OPEN ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
 │            │
 └─→ CANCELLED └─→ CANCELLED
```

- Implemented in `TicketStatus.canTransitionTo()` + `TicketService.changeStatus()`.
- Same-status PATCH is a no-op (returns current ticket).
- Every other transition → `InvalidStatusTransitionException` → HTTP 422 with message
  listing allowed transitions.
- Frontend shows all statuses as buttons; disallowed ones intentionally remain clickable
  so reviewers can observe the backend 422 rejection.

## 4. API design

Base URL: `http://localhost:8080`. All JSON.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tickets` | Create ticket → 201 + body |
| GET | `/api/tickets?status=&q=` | List, optional status filter + keyword search |
| GET | `/api/tickets/{id}` | Details incl. comments |
| PUT | `/api/tickets/{id}` | Update title/description/priority/assignee |
| PATCH | `/api/tickets/{id}/status` | Body `{"status":"IN_PROGRESS"}` → 200 or 422 |
| POST | `/api/tickets/{id}/comments` | Add comment → 201 |
| GET | `/api/tickets/{id}/comments` | List comments |
| DELETE | `/api/tickets/{id}` | Delete → 204 |

Error envelope:
- 400 validation: `{"timestamp","status":400,"error":"Validation failed","fields":{...}}`
- 404: `{"timestamp","status":404,"error":"Ticket 99 not found"}`
- 422: `{"timestamp","status":422,"error":"Invalid status transition from ..."}`
- Unknown enum value → 400 via `IllegalArgumentException` handler.

Example — create:
```bash
curl -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Login broken","description":"500 on submit","priority":"HIGH","assignee":"ada"}'
```

## 5. Validation rules

| Field | Rule | Error |
|-------|------|-------|
| ticket.title | required, 1..200 | 400 `fields.title` |
| ticket.description | required, 1..4000 | 400 `fields.description` |
| ticket.priority | optional enum, default MEDIUM | 400 on unknown value |
| ticket.assignee | optional, ≤100, blank→null | 400 `fields.assignee` |
| status body | required enum | 400 if missing/unknown; 422 if illegal transition |
| comment.body | required, 1..2000 | 400 `fields.body` |
| comment.author | optional, ≤100 | 400 `fields.author` |

Frontend mirrors `required`/`maxLength` attributes and renders server messages in an error banner.

## 6. UI/UX

- Single-page app (`frontend/src/App.jsx`):
  1. **Create form** (title*, description*, priority, assignee).
  2. **List** with search box + status dropdown + Apply/Reset; table rows clickable.
  3. **Detail view**: status buttons (all five), allowed-next hint, edit form, comment list + add-comment form, back button.
- Error banner at top of page; dismissible; shows joined `fields` messages or 422 text.
- No router dependency — view state (`selected`) toggles list/detail to stay minimal.

## 7. Persistence

- H2 file DB: `spring.datasource.url=jdbc:h2:file:./data/ticketdb;AUTO_SERVER=TRUE`
  (relative to `backend/` working dir → `backend/data/`).
- `ddl-auto=update`; data survives backend restarts. H2 console at `/h2-console`.
- Acceptance check: create ticket → stop backend → start backend → ticket still listed.

## 8. AI context management (how spec-driven dev was used here)

1. **Spec first**: this file was the source of truth before code.
2. **Scaffold from spec**: prompts referenced SPEC sections (domain → entities, §3 → service, §4 → controller, §5 → DTOs).
3. **Small diffs**: entities → repos → service → web → frontend; each step independently explainable.
4. **Verify against spec**: state-machine tests map 1:1 to §3 transitions; acceptance criteria (§10) checked manually via UI + curl.
5. Suggested Cursor prompts: *"Implement §2 entities"*, *"Implement §3 in TicketService"*, *"Generate tests for every allowed/forbidden edge in §3"*.

## 9. Testing / debugging strategy

- `TicketStateMachineIT` (SpringBootTest + MockMvc, `@Transactional`):
  - valid: full lifecycle OPEN→…→CLOSED; OPEN→CANCELLED; IN_PROGRESS→CANCELLED.
  - invalid (expect 422): OPEN→RESOLVED; CLOSED→OPEN; RESOLVED→OPEN; CANCELLED→OPEN.
  - validation (expect 400): empty title/description; empty comment body.
- Run: `cd backend && mvn test`.
- Manual: create via UI → walk valid transitions → attempt invalid ones (expect banner with 422 text) → restart backend → confirm persistence.
- Debug tips: H2 console (`jdbc:h2:file:./data/ticketdb`, user `sa`); backend logs; browser devtools network tab for 400/422 bodies.

## 10. Acceptance criteria

- [ ] All features (F1–F11) work from the UI.
- [ ] Valid transitions succeed; invalid ones rejected by backend (422 visible in UI).
- [ ] Data survives backend restart.
- [ ] Validation errors shown meaningfully (no silent failures).
- [ ] `mvn test` green (state-machine integration tests).
- [ ] No secrets committed (`git status` / secret scan clean).

## 11. File map

```
SPEC.md
README.md
backend/pom.xml
backend/src/main/java/com/example/ticketai/
  TicketAiApplication.java
  domain/{Ticket,TicketStatus,Priority,Comment}.java
  repository/{TicketRepository,CommentRepository}.java
  service/{TicketService,NotFoundException,InvalidStatusTransitionException}.java
  web/{TicketController,GlobalExceptionHandler}.java
  web/dto/{TicketRequest,TicketUpdateRequest,StatusChangeRequest,CommentRequest,TicketResponse,CommentResponse}.java
backend/src/main/resources/application.properties
backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java
backend/src/test/resources/application.properties  (in-memory H2, isolates tests from dev file DB)
frontend/{package.json,vite.config.js,index.html}
frontend/src/{main.jsx,App.jsx,api.js,styles.css}
```
