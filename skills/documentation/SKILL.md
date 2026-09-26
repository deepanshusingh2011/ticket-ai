# Documentation Skill — TicketAI Specs

Applies to: `SPEC.md`, `docs/SPEC_CONSOLIDATED.md`, `spec/*`, `docs/prompt-history.md`.
Read this before updating any spec or history doc.

## File roles (never confuse them)

- `SPEC.md` (repo root) — v1 history. FROZEN. Never edit, never rewrite, never delete.
- `docs/SPEC_CONSOLIDATED.md` — the single self-contained build contract (backend + frontend).
  An agent with ONLY this file must rebuild everything. This is the file you update.
- `spec/` (`requirements.md`, `architecture.md`, `data-model.md`, `state-machine.md`,
  `api-contract.md`, `ui-flow.md`, `test-strategy.md`) — detail views derived FROM the
  consolidated spec. Keep consistent with it; never let them disagree.
- `docs/prompt-history.md` — append-only log of prompts/build steps. Append, don't rewrite.

## Update procedure

1. Make the change in `docs/SPEC_CONSOLIDATED.md` FIRST (numbered §1–§14 structure;
   keep endpoint table §6.1, envelope §6.2, error shapes §6.3, curls §6.5 in sync).
2. Mirror it into the matching `spec/*.md` detail file(s):
   endpoint change → `spec/api-contract.md`; transition change → `spec/state-machine.md`;
   field change → `spec/data-model.md` + `spec/requirements.md`; test change → `spec/test-strategy.md`.
3. If behavior changed, update the acceptance checklist (§12) and verification steps (§14).
4. Append one entry to `docs/prompt-history.md` (date, what changed, why). Never rewrite history.
5. Verify consistency: `status`/`q`/`page`/`size`/`sort` names, envelope keys
   (`content,page,size,totalElements,totalPages`), status codes (201/200/204/400/404/422),
   transition table — identical in all three layers (consolidated, `spec/*`, backend code).

## Guardrails

- Do NOT touch `SPEC.md`. If tempted, you are editing the wrong file.
- Do NOT overwrite `docs/SPEC_CONSOLIDATED.md` from a `spec/*` fragment — direction is
  consolidated → fragments, never reverse.
- Do NOT “clean up” `docs/prompt-history.md` (no squashing, no rewording old entries).
- Keep code-derived facts exact: `canTransitionTo()` table, error envelope shapes from
  `GlobalExceptionHandler`, CORS origins from `TicketController`, H2 URLs from
  `application.properties`, the 9 test names from `TicketStateMachineTest`.
- After editing, run a consistency grep for the changed term across
  `docs/SPEC_CONSOLIDATED.md`, `spec/`, and `backend/src/main` before finishing.
