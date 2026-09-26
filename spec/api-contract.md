# API Contract

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §6). Focused: endpoints, pagination envelope, error shapes, curl per endpoint.

Base URL: `http://localhost:8080`. All requests/responses JSON. `Content-Type: application/json` on all bodies.

## Endpoint table

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

Expected codes: create 201 (+ `Location: /api/tickets/{id}`), paged/legacy list 200, details 200, update 200, status-change 200 or 422 (400 on bad enum), add-comment 201 (+ `Location: /api/tickets/{id}/comments/{commentId}`), list-comments 200, delete 204.

## Pagination params & envelope (endpoint #3)

Request params:

- `status` (optional enum): exact match filter.
- `q` (optional string, param name is `q`): keyword search, case-insensitive partial on title+description.
- `page` (optional int, 0-based, default `0`).
- `size` (optional int, default `10`).
- `sort` (optional, repeatable `sort=property,dir`, e.g. `sort=id,desc`): Spring `Sort` syntax; default `id,desc` when absent/blank.

Mode rule (controller): if **none** of `page/size/sort` are present → return bare JSON array (legacy). If **any** is present → return the envelope below.

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

## Error envelope

- `400` validation: `{"timestamp":"…","status":400,"error":"Validation failed","fields":{"title":"…"}}`
- `404` missing ticket: `{"timestamp":"…","status":404,"error":"Ticket 99 not found"}`
- `422` illegal transition: `{"timestamp":"…","status":422,"error":"Invalid status transition from OPEN to RESOLVED. Allowed: [IN_PROGRESS, CANCELLED]"}`
- Unknown enum value (e.g. `"priority":"P9"`) → `400` via `IllegalArgumentException` handler.

## Shapes

Create (`POST /api/tickets`) — `priority`/`assignee` optional:

```json
{ "title": "Login broken", "description": "500 on submit", "priority": "HIGH", "assignee": "ada" }
```

Update (`PUT /api/tickets/{id}`) — all four writable fields:

```json
{ "title": "Login broken (upd)", "description": "500 on submit", "priority": "URGENT", "assignee": "ada" }
```

Status change (`PATCH /api/tickets/{id}/status`):

```json
{ "status": "IN_PROGRESS" }
```

Add comment (`POST /api/tickets/{id}/comments`):

```json
{ "body": "Repro confirmed", "author": "ada" }
```

## curl per endpoint (run against local backend)

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
