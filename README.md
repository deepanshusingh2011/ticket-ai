# Support Ticket Management System

Minimal ticket tracker: Spring Boot REST API + Vite React UI + H2 file DB.
Full design spec: [`SPEC.md`](SPEC.md).

## How spec-driven dev was used
1. Wrote `SPEC.md` first (requirements, domain model, state machine, API, validation).
2. Scaffolded backend/frontend directly from spec sections.
3. Verified with state-machine integration tests + manual UI walkthrough vs. acceptance criteria.

## Prerequisites
- Java 21 (`java -version`), Maven 3.6+ (`mvn -v`), Node 18+ (`node -v`).

## Run the backend
```bash
cd backend
mvn spring-boot:run
# API at http://localhost:8080, H2 console at http://localhost:8080/h2-console
# (JDBC URL: jdbc:h2:file:./data/ticketdb, user: sa, empty password)
```

## Run the frontend

> The UI now lives in its own repo: `ticket_frontend/ticket_frontend_ai`
> (sibling of this repo). The `frontend/` directory kept here is a legacy
> copy — do not extend it; the standalone repo is the source of truth.

```bash
cd ../ticket_frontend/ticket_frontend_ai
npm install
npm run dev
# UI at http://localhost:5174 (proxies /api to :8080, or use CORS directly)
```

## How to test
```bash
cd backend
mvn test   # state-machine integration tests (valid + invalid transitions, validation)
```

Manual checks:
- Create ticket → list → open detail → edit fields → add comments.
- Walk OPEN → IN_PROGRESS → RESOLVED → CLOSED (works); try CLOSED → OPEN (expect 422 banner).
- Restart backend, confirm tickets persist (H2 file in `backend/data/`).

## API quick reference
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/tickets` | create → 201 |
| GET | `/api/tickets?status=&q=` | list / filter / search |
| GET | `/api/tickets/{id}` | details + comments |
| PUT | `/api/tickets/{id}` | update fields |
| PATCH | `/api/tickets/{id}/status` | `{"status":"…"}` → 200 or 422 |
| POST/GET | `/api/tickets/{id}/comments` | comments |

State machine: `OPEN → IN_PROGRESS | CANCELLED`, `IN_PROGRESS → RESOLVED | CANCELLED`, `RESOLVED → CLOSED`. All else → 422.
