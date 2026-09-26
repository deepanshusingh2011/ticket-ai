# CHAT_HISTORY.md — Build Log (Chronological)

> Companion to [`SPEC.md`](../SPEC.md) (source of truth) and [`README.md`](../README.md).
> This file only summarizes history — no existing files were modified to create it.

## Contents

- [1. Initial request](#1-initial-request--ticket-system--stack)
- [2. Spec creation](#2-spec-creation)
- [3. Backend scaffolding](#3-backend-scaffolding)
- [4. Frontend scaffolding](#4-frontend-scaffolding)
- [5. Running verification](#5-running-verification)
- [6. PR split + backup branch](#6-pr-split--backup-branch)
- [7. Visibility fix](#7-visibility-fix)
- [8. Endpoint curls + Postman](#8-endpoint-curls--postman-collection)
- [9. This docs request](#9-this-docs-request)
- [Key commands](#key-commands)

## 1. Initial request — ticket system + stack

Requested a minimal support-ticket tracker for small teams (create → triage →
comment → search), with a fixed stack:

- Backend: Java 21, Spring Boot 3.2.x, H2 (file-based), REST API on `:8080`
- Frontend: Vite + React 18 on `:5173`
- No auth in v1 (explicit non-goal)

## 2. Spec creation

`SPEC.md` was written **before any code** as the source of truth (§§1–11):
requirements F1–F11, domain model, backend-enforced state machine (§3),
API design (§4), validation rules (§5), UI/UX (§6), H2 persistence (§7),
testing strategy (§9), acceptance criteria (§10), file map (§11).

## 3. Backend scaffolding

Generated from SPEC sections, smallest diffs first:

1. Domain: `Ticket`, `TicketStatus` (`canTransitionTo()`), `Priority`, `Comment`
2. Repositories: `TicketRepository`, `CommentRepository`
3. Service: `TicketService` (+ `changeStatus()` enforcement,
   `NotFoundException`, `InvalidStatusTransitionException`)
4. Web: `TicketController`, `GlobalExceptionHandler`, DTOs
   (`TicketRequest`, `TicketUpdateRequest`, `StatusChangeRequest`,
   `CommentRequest`, `TicketResponse`, `CommentResponse`)
5. Config: `application.properties` (H2 file DB, H2 console, CORS for
   `:5173`/`:3000`)

Full reference: [`docs/BACKEND.md`](BACKEND.md).

## 4. Frontend scaffolding

Single-page app, no router (`selected` state toggles list/detail):

- `frontend/src/App.jsx` — create form, list + search/filter, detail view,
  status buttons, edit form, comments, error banner
- `frontend/src/api.js` — `fetch` wrapper (`list/get/create/update/changeStatus/addComment`)
- `frontend/src/main.jsx`, `styles.css`, Vite proxy `/api → :8080`

Full reference: [`docs/FRONTEND.md`](FRONTEND.md).

## 5. Running verification

- Backend: `mvn spring-boot:run` → API on `http://localhost:8080`
- Frontend: `npm run dev` → UI on `http://localhost:5173`
- Tests: `mvn test` → **9 state-machine integration tests green**
  (valid lifecycles, 422 on illegal transitions, 400 on validation failures)
- Persistence: create ticket → stop backend → restart → ticket still listed
  (H2 file in `backend/data/`)

## 6. PR split + backup branch

Work was split into **5 reviewable slices** (entities → repos → service →
web → frontend), each independently explainable per the spec-driven workflow.
A snapshot lives on branch **`backup/main-work-20260926`**.

## 7. Visibility fix

Fixed a visibility/reachability issue so the running UI and API were
accessible for verification (frontend proxy/CORS alignment with the backend).

## 8. Endpoint curls + Postman collection

All 8 endpoints verified with `curl` (see [Key commands](#key-commands) and
[`docs/BACKEND.md`](BACKEND.md#api--curl-examples)). Saved as the Postman
collection `postman/TicketAI.postman_collection.json` (existing file —
not modified by this docs task).

## 9. This docs request

Created `docs/CHAT_HISTORY.md` (this file), `docs/BACKEND.md`, and
`docs/FRONTEND.md` — **new files only**; `SPEC.md`, `README.md`, and the
Postman collection were left untouched. Not committed or pushed.

## Key commands

```bash
# Backend
cd backend
mvn spring-boot:run        # API at http://localhost:8080

# Frontend
cd frontend
npm install
npm run dev                # UI at http://localhost:5173

# Tests
cd backend
mvn test                   # 9 state-machine integration tests
```

```bash
# Git — snapshot / slices (illustrative)
git branch backup/main-work-20260926
git checkout -b cursor/<ticket>-<summary>
git add <slice-files> && git commit -m "<why, not what>"
```

```bash
# Curl — create + list + search
curl -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Login broken","description":"500 on submit","priority":"HIGH","assignee":"ada"}'

curl 'localhost:8080/api/tickets?status=OPEN&q=login'
```

```bash
# Curl — detail, update, status change, comment
curl localhost:8080/api/tickets/1
curl -X PUT localhost:8080/api/tickets/1 -H 'Content-Type: application/json' \
  -d '{"title":"Login broken (upd)","description":"500 on submit","priority":"URGENT","assignee":"ada"}'
curl -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
curl -X POST localhost:8080/api/tickets/1/comments -H 'Content-Type: application/json' \
  -d '{"body":"Repro confirmed","author":"ada"}'
```
