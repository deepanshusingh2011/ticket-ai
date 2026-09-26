# UI Flow

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §10). Focused: routes, pages, pagination UX, error handling.

## Routes (react-router-dom@6, in `src/App.jsx`)

| Route | Page | Purpose |
|-------|------|---------|
| `/` | `ListPage` | Ticket table + search/filter + pagination |
| `/create` | `CreatePage` | Create form → POST → navigate to `/tickets/{id}` |
| `/tickets/:id` | `DetailPage` | Detail + status buttons + comments + delete |
| `/tickets/:id/edit` | `EditPage` | Edit form (PUT) → navigate back to detail |
| `*` | → `ListPage` | Fallback |

Header/nav on every page: title `Support Ticket System`, meta line `Backend: Spring Boot :8080 · Frontend: Vite :5174 · DB: H2 file`, nav buttons `Tickets → /`, `+ New ticket → /create`.

## API client (`src/api.js`)

- `BASE = import.meta.env.VITE_API_URL ?? ''` (relative URLs through Vite proxy by default).
- `parseError(res)`: if body has `fields`, join as `k: v; …`; else `error` text; fallback `Request failed ({status})`. `handle(res)`: throw `Error(parsed)` on non-OK; `204 → null`.
- `toPage(data)`: paged envelope returned as-is; **bare array** wrapped to `{content, page:0, size:length, totalElements:length, totalPages:1}` so list UI treats both shapes uniformly.
- Exports: `list({status, q, page, size, sort})`, `get(id)`, `create(payload)`, `update(id, payload)`, `changeStatus(id, status)`, `remove(id)`, `addComment(id, payload)`, plus `STATUSES`, `PRIORITIES`, `PAGE_SIZES = [5,10,20]`, `allowedNext(status)`.

## Pages & pagination UX

**ListPage (`/`)** — state: `tickets, status, query, page (0-based), size (default 10), totalElements, totalPages, error, loading`.

- On mount: `refresh(0, size, '', '')`.
- `refresh(pageArg, sizeArg, statusArg, qArg)` calls `api.list(...)` with `status||undefined, q||undefined, page, size`; sets `tickets = data.content || []` plus page metadata.
- Filter bar: search input (`placeholder "keyword in title/description"`), status dropdown (`All` + five statuses), `Apply` (resets to page 0, refetches), `Reset` (clears both, page 0), `+ New ticket` link.
- Table columns: ID, Title, Status (pill), Priority, Assignee (`—` when null). Row click → `navigate(/tickets/{id})`. Empty state: `No tickets yet. Create one.` Loading state: `Loading…`.
- Pagination controls (always rendered with the table):
  - Label: `Showing {from}–{to} of {totalElements} · page {page+1} of {totalPages}` (`from = totalElements===0 ? 0 : page*size+1`, `to = min(total, page*size+tickets.length)`).
  - Page-size selector (`5/10/20 per page`) → resets to page 0.
  - `← Prev` / numbered window (`pageWindow(current,total,width=5)`, 1-based labels, current disabled) / `Next →` (Prev disabled at first page; Next disabled at last/empty). Out-of-range `goTo()` clamps.

**CreatePage (`/create`)** — controlled form: `title*` (`required`, `maxLength 200`), `description*` (`required`, `maxLength 4000`, textarea), `priority` select (default MEDIUM), `assignee` input (`maxLength 100`, blank → null/omit). Submit → `api.create` → navigate to `/tickets/{id}`; failure → error banner. Cancel/back link to `/`.

**DetailPage (`/tickets/:id`)** — loads via `api.get(id)` with `loading` state; `Ticket not found.` + back link when null.

- Meta line: `Status: X · Priority: Y · Assignee: Z`.
- Status buttons: all five `STATUSES` rendered; current disabled; others clickable with `title = allowed ? 'Allowed transition' : 'Will be rejected by backend'`; click → `api.changeStatus` → refresh or 422 banner.
- Hint: `Allowed next: {allowedNext(status).join(', ') || 'none (terminal state)'}. Other buttons demonstrate backend rejection.`
- `Edit → /tickets/{id}/edit`; `Delete` → `confirm(...)` → `api.remove` → `navigate('/')`.
- Comments: `Comments (n)` list (`author || 'anonymous'`, locale date, body) + add form (`body*` textarea `maxLength 2000 required`, `author` input `maxLength 100 optional`) → POST → clear + reload.

**EditPage (`/tickets/:id/edit`)** — preloads via `api.get(id)`; same field rules as create; submit → `api.update(id, {title, description, priority, assignee})` → navigate to `/tickets/{id}`.

## Error handling (all pages)

- Shared `ErrorBanner` (defined in `ListPage.jsx`, reused by Detail): top of page, shows `err.message` (already-joined 400 `fields` text or raw 422 text), dismissible `×` button, renders nothing when empty. Every fetch failure sets the banner — no silent failures.
- Client mirrors backend with `required`/`maxLength` attributes but ALWAYS surfaces server messages (server is authoritative).
