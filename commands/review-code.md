# /review-code — TicketAI Code Review

Checklist + prompt for reviewing backend changes against `docs/SPEC_CONSOLIDATED.md`.

## Checklist

- [ ] State machine: all transition logic in `TicketStatus.canTransitionTo()` + `TicketService.changeStatus()` only;
      same-status PATCH is a no-op (200); illegal → `InvalidStatusTransitionException` → 422 with allowed list.
- [ ] PUT never touches status; status changes only via `PATCH /{id}/status`.
- [ ] Validation: `@Valid` on all `@RequestBody` DTOs; blank assignee/author → null; error shapes
      400+`fields` / 404 / 422 / bad-enum→400 per `GlobalExceptionHandler`.
- [ ] List dual-mode: no page/size/sort → bare array; any present → envelope
      (`content,page,size,totalElements,totalPages`); default sort `id,desc`.
- [ ] Persistence: dev file DB vs test in-memory H2; no `backend/data/` committed; no secrets.
- [ ] CORS: per-controller `@CrossOrigin` keeps `5174/5173/3000`; no global `*`.
- [ ] Java 21, Boot 3.2.x, existing package/DTO (`from()`) and naming conventions followed; minimal diff.

## Prompt

```text
Review this TicketAI backend diff against docs/SPEC_CONSOLIDATED.md and rules/java-springboot.md
+ rules/api-standards.md. Files: <list changed files>. For each checklist item above report
PASS/FAIL with file:line evidence. Flag 400-vs-422 confusion, status-in-PUT, transition logic
outside TicketStatus/TicketService, envelope key changes, CORS widening, or file-DB leakage
into tests. Do not fix; report findings ordered by severity.
```
