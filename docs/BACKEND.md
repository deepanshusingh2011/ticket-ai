# BACKEND.md — Spring Boot Ticket API

> Summary companion to [`SPEC.md`](../SPEC.md) (§§2–5, §7, §9), the source of truth.
> Covers: `backend/` only.

## Contents

- [Requirements](#requirements)
- [Domain model](#domain-model)
- [State machine](#state-machine)
- [API + curl examples](#api--curl-examples)
- [Validation rules](#validation-rules)
- [Persistence](#persistence)
- [Tests](#tests)
- [How to run](#how-to-run)

## Requirements

F1 create · F2 list · F3 details+comments · F4 update fields ·
F5 status only via allowed transitions (backend-enforced) · F6 comments ·
F7 keyword search (title+description, case-insensitive, partial) ·
F8 filter by status · F9 status+keyword combined · F10 persist across restarts ·
F11 validation with meaningful errors.

Non-goals (v1): auth, attachments, SLA timers, notifications, pagination, roles.

## Domain model

```
Ticket (tickets)
  id: Long PK auto-increment
  title: String 1..200 (required)
  description: String 1..4000 (required)
  status: TicketStatus (default OPEN)
  priority: Priority (default MEDIUM)
  assignee: String ≤100, nullable (blank → null)
  createdAt / updatedAt: Instant
  comments: 1:N cascade ALL, orphanRemoval

Comment (comments)
  id: Long PK · ticket_id FK (required)
  body: String 1..2000 (required)
  author: String ≤100, nullable
  createdAt: Instant immutable

TicketStatus: OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED
Priority: LOW | MEDIUM | HIGH | URGENT
```

Key files: `domain/{Ticket,TicketStatus,Priority,Comment}.java`,
`repository/{TicketRepository,CommentRepository}.java`,
`service/TicketService.java`, `web/{TicketController,GlobalExceptionHandler}.java`,
`web/dto/` (`TicketRequest`, `TicketUpdateRequest`, `StatusChangeRequest`,
`CommentRequest`, `TicketResponse`, `CommentResponse`).

## State machine

```
OPEN ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
 │            │
 └─→ CANCELLED └─→ CANCELLED
```

Enforced in `TicketStatus.canTransitionTo()` + `TicketService.changeStatus()`.
Same-status PATCH is a no-op (returns current ticket); any other illegal
transition → `InvalidStatusTransitionException` → **HTTP 422** with allowed
transitions listed. `CLOSED`/`CANCELLED` are terminal.

## API + curl examples

Base URL `http://localhost:8080`, all JSON. Error envelope: 400
`{"timestamp","status":400,"error":"Validation failed","fields":{...}}`;
404 `{"…","status":404,"error":"Ticket 99 not found"}`;
422 `{"…","status":422,"error":"Invalid status transition from …"}`.

| Method | Path | Result |
|--------|------|--------|
| POST | `/api/tickets` | 201 + ticket |
| GET | `/api/tickets?status=&q=` | 200 list (filter/search) |
| GET | `/api/tickets/{id}` | 200 details + comments |
| PUT | `/api/tickets/{id}` | 200 updated (title/desc/priority/assignee) |
| PATCH | `/api/tickets/{id}/status` | 200 or 422, body `{"status":"…"}` |
| POST | `/api/tickets/{id}/comments` | 201 comment |
| GET | `/api/tickets/{id}/comments` | 200 comment list |
| DELETE | `/api/tickets/{id}` | 204 |

```bash
curl -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Login broken","description":"500 on submit","priority":"HIGH","assignee":"ada"}'

curl 'localhost:8080/api/tickets?status=OPEN&q=login'
curl localhost:8080/api/tickets/1

curl -X PUT localhost:8080/api/tickets/1 -H 'Content-Type: application/json' \
  -d '{"title":"Login broken (upd)","description":"500 on submit","priority":"URGENT","assignee":"ada"}'

curl -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
# illegal transition, e.g. OPEN → RESOLVED, returns 422

curl -X POST localhost:8080/api/tickets/1/comments -H 'Content-Type: application/json' \
  -d '{"body":"Repro confirmed","author":"ada"}'
curl localhost:8080/api/tickets/1/comments

curl -X DELETE localhost:8080/api/tickets/1  # → 204
```

## Validation rules

| Field | Rule | Failure |
|-------|------|---------|
| `ticket.title` | required, 1..200 | 400 `fields.title` |
| `ticket.description` | required, 1..4000 | 400 `fields.description` |
| `ticket.priority` | optional enum, default MEDIUM | 400 on unknown value |
| `ticket.assignee` | optional, ≤100, blank→null | 400 `fields.assignee` |
| status body | required enum | 400 if missing/unknown; 422 if illegal transition |
| `comment.body` | required, 1..2000 | 400 `fields.body` |
| `comment.author` | optional, ≤100 | 400 `fields.author` |

## Persistence

H2 file DB: `jdbc:h2:file:./data/ticketdb;AUTO_SERVER=TRUE` (→ `backend/data/`),
`ddl-auto=update`, console at `/h2-console` (user `sa`, empty password, dev only).
Data survives restarts — acceptance check: create → stop → start → still listed.
CORS restricted to `http://localhost:5173` and `http://localhost:3000`.

> Note: the working tree currently overrides `server.port=8081` in
> `backend/src/main/resources/application.properties` (uncommitted); SPEC default is `8080`.

## Tests

State-machine integration tests (`SpringBootTest` + MockMvc, `@Transactional`;
in-memory H2 via `src/test/resources/application.properties`):

- Valid: `OPEN→IN_PROGRESS→RESOLVED→CLOSED`, `OPEN→CANCELLED`, `IN_PROGRESS→CANCELLED`
- Invalid → 422: `OPEN→RESOLVED`, `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`
- Validation → 400: empty title/description, empty comment body

```bash
cd backend
mvn test   # 9 tests, green
```

## How to run

```bash
cd backend
mvn spring-boot:run
# API at http://localhost:8080, H2 console at http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:./data/ticketdb, user: sa, empty password
```
