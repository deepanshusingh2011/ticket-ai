# FRONTEND.md — Vite + React Ticket UI

> NOTE: the UI has moved to the standalone repo
> `ticket_frontend/ticket_frontend_ai` (sibling of `ticket-ai`, dev port
> `:5174`). The `frontend/` tree described below is the legacy in-place copy.

> Summary companion to [`SPEC.md`](../SPEC.md) (§6 UI/UX, §5 validation display),
> the source of truth. Covers: `frontend/` only.

## Contents

- [Requirements](#requirements)
- [Structure](#structure)
- [Pages / views](#pages--views)
- [Components / features](#components--features)
- [API client](#api-client)
- [How to run](#how-to-run)
- [Manual checks](#manual-checks)

## Requirements

Single-page app, no router: create tickets, list with search + status filter,
detail view with status buttons / edit form / comments, error banner for
server messages (400 `fields` / 422 text). Mirrors backend `required`/
`maxLength` attributes client-side. Talks to backend via `/api` proxy (or CORS).

## Structure

```
frontend/
  package.json · vite.config.js (proxy /api → http://localhost:8080) · index.html
  src/
    main.jsx     # React root
    App.jsx      # whole UI: create form, list, detail, error banner
    api.js       # fetch wrapper + STATUSES/PRIORITIES/allowedNext()
    styles.css
```

## Pages / views

| View | Content |
|------|---------|
| List (default) | Create form → search box + status dropdown + Apply/Reset → ticket table (rows clickable → detail) |
| Detail (`selected` set) | Status buttons (all five) + allowed-next hint → edit form → comment list + add-comment form → back button |

View state is a single `selected` ticket toggle — no routing dependency.

## Components / features

- **Create form** — `title*`, `description*`, `priority` (default MEDIUM), `assignee`.
- **Search / filter** — keyword `q` (title+description, case-insensitive) + `status`
  dropdown; combinable; Reset clears both.
- **Detail** — shows all fields + comments; edit form updates
  title/description/priority/assignee via PUT.
- **Status buttons** — all five statuses intentionally clickable so reviewers can
  observe backend 422 rejections; `allowedNext()` hint shows legal moves.
- **Comments** — list + add form (`body*`, `author`).
- **Error banner** — top of page, dismissible; joins 400 `fields` messages or
  shows 422 transition text; no silent failures.

## API client

`src/api.js` (`BASE = ''`, relative URLs through Vite proxy):

| Function | Request |
|----------|---------|
| `api.list({status, q})` | `GET /api/tickets?status=&q=` |
| `api.get(id)` | `GET /api/tickets/{id}` |
| `api.create(payload)` | `POST /api/tickets` → 201 |
| `api.update(id, payload)` | `PUT /api/tickets/{id}` |
| `api.changeStatus(id, status)` | `PATCH /api/tickets/{id}/status` → 200 or 422 |
| `api.addComment(id, payload)` | `POST /api/tickets/{id}/comments` → 201 |

Non-OK responses throw `Error` with the parsed server message; `204` → `null`.
Exports `STATUSES`, `PRIORITIES`, and `allowedNext(status)` hint helper.

## How to run

```bash
cd frontend
npm install
npm run dev
# UI at http://localhost:5173 (proxies /api to :8080, or use CORS directly)
```

Backend must be running first (`cd backend && mvn spring-boot:run`).

## Manual checks

1. Create ticket → appears in list → open detail → edit fields → add comments.
2. Walk `OPEN → IN_PROGRESS → RESOLVED → CLOSED` (succeeds); try
   `CLOSED → OPEN` (expect 422 error banner).
3. Search keyword + status filter, combined and reset.
4. Restart backend → tickets persist (H2 file in `backend/data/`).
5. Debug: browser devtools network tab for 400/422 bodies; H2 console at
   `http://localhost:8080/h2-console`.
