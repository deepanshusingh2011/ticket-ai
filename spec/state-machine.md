# State Machine

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §5). Focused: allowed transitions and enforcement.

## Diagram

```
OPEN ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
 │            │
 └─→ CANCELLED └─→ CANCELLED
```

## Transition table

| From | Allowed next |
|------|--------------|
| OPEN | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | RESOLVED, CANCELLED |
| RESOLVED | CLOSED |
| CLOSED | — (terminal) |
| CANCELLED | — (terminal) |

## Rules

- Enforced in `TicketStatus.canTransitionTo()` + `TicketService.changeStatus()`.
- Same-status PATCH is a no-op → `200` with current ticket (not an error).
- Every other transition → `InvalidStatusTransitionException` → **HTTP 422** with body `{"timestamp","status":422,"error":"Invalid status transition from X to Y. Allowed: [...]"}`.
- `CLOSED` and `CANCELLED` are terminal: any change attempt (to a different status) → 422.
- Unknown enum value in the status body → 400, not 422.
- Frontend MUST render all five statuses as clickable buttons so reviewers can observe the backend 422 rejection, plus an `allowedNext()` hint line.

## Enforcement points

1. Domain: `TicketStatus.canTransitionTo(target)` — pure function, exact table above.
2. Service: `TicketService.changeStatus(id, target)` — load or 404; if `current == target` return as-is; else if `!current.canTransitionTo(target)` throw with message naming the transition and allowed targets.
3. Web: `GlobalExceptionHandler` maps the exception to 422 with the error envelope.
4. UI: `allowedNext(status)` helper mirrors the table for the hint line only — the server remains authoritative (all buttons stay clickable).
