# API Standards — TicketAI

Source of truth: `docs/SPEC_CONSOLIDATED.md` §6 + `TicketController` + `GlobalExceptionHandler`.

## REST paths (`/api/tickets` base)

| # | Method | Path | Success | Notes |
|---|--------|------|---------|-------|
| 1 | POST | `/api/tickets` | 201 + ticket, `Location: /api/tickets/{id}` | priority/assignee optional |
| 2 | GET | `/api/tickets?status=&q=` | 200 bare array | legacy: NO page/size/sort → full list |
| 3 | GET | `/api/tickets?status=&q=&page=&size=&sort=` | 200 pagination envelope | ANY of page/size/sort → envelope |
| 4 | GET | `/api/tickets/{id}` | 200 ticket + comments | 404 if missing |
| 5 | PUT | `/api/tickets/{id}` | 200 ticket | title/desc/priority/assignee only — NEVER status |
| 6 | PATCH | `/api/tickets/{id}/status` | 200 ticket, or 422 | body `{"status":"IN_PROGRESS"}` |
| 7 | POST | `/api/tickets/{id}/comments` | 201 comment, `Location: /api/tickets/{id}/comments/{cid}` | |
| 8 | GET | `/api/tickets/{id}/comments` | 200 comment array | ordered by `createdAt` asc |
| 9 | DELETE | `/api/tickets/{id}` | 204 empty | |

- Search param is `q` (matches title OR description, case-insensitive, partial `LIKE %q%`).
  Status filter is exact enum. Both optional and combinable. Default list ordering `id DESC`.
- Sort syntax: repeatable `sort=property,dir` (e.g. `sort=id,desc`); blank/absent → `id,desc`.
- All requests/responses JSON with `Content-Type: application/json`. Timestamps ISO-8601 instants.

## Pagination envelope (endpoint #3)

Mode rule (controller): none of `page/size/sort` present → bare array; any present → envelope.

```json
{
  "content": [ { "id": 1, "title": "…", "status": "OPEN", "priority": "HIGH", "assignee": "ada", "createdAt": "…", "updatedAt": "…", "description": "…", "comments": [] } ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

Keys are exactly `content, page, size, totalElements, totalPages` (0-based `page`).

## DTO naming

- Request: `TicketRequest` (create), `TicketUpdateRequest` (PUT — no status field),
  `StatusChangeRequest` (PATCH — `status` only), `CommentRequest` (`body` + optional `author`).
- Response: `TicketResponse.from(entity)` → `id, title, description, status, priority, assignee,
  createdAt, updatedAt, comments[]`; `CommentResponse.from(entity)` → `id, body, author, createdAt`.
- Map entity→response via static `from()` factory methods, never expose JPA entities directly.

## Status codes

- `201` create ticket / add comment (with `Location` header). `200` all reads, PUT, valid PATCH.
- `204` delete (empty body). Same-status PATCH → `200` no-op, not an error.
- `400` Bean Validation failure (`fields` map) + unknown enum value (`IllegalArgumentException`).
- `404` unknown ticket id (`Ticket {id} not found`). `422` legal enum but illegal transition.
- Error envelope shape: `{"timestamp","status","error"}` (+ `fields` map only on 400-validation).

## Changing the contract

- New endpoint/query param → update `docs/SPEC_CONSOLIDATED.md` §6 + `spec/api-contract.md` +
  `postman/TicketAI.postman_collection.json` curls in the same change.
- Never rename envelope keys, the `q` param, or DTO field names without updating the standalone
  frontend (`src/api.js` `toPage()` depends on both bare-array and envelope shapes).
