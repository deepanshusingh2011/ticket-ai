# Plan / Tasks — Support Ticket Management System

> Staged task breakdown mapping the required workflow
> Requirement → Specification → Plan/Tasks → Implementation → Testing → Review → Fix.
> Derived from `docs/SPEC_CONSOLIDATED.md` (§§1–3, 12–14), `SPEC.md`, and `docs/prompt-history.md`.
> This file is the Plan/Tasks artefact; it adds no new requirements.

## Stage 1 — Requirement (DONE)

- [x] Capture problem statement + users + F1–F11 (later F12–F13) and non-goals.
- Source: `SPEC.md` §1; consolidated in `docs/SPEC_CONSOLIDATED.md` §1.
- Evidence: `spec/requirements.md` (F1–F13 table + acceptance hints).

## Stage 2 — Specification (DONE)

- [x] Domain model (`spec/data-model.md` ← consolidated §4).
- [x] State machine (`spec/state-machine.md` ← consolidated §5).
- [x] API contract + pagination envelope + curls (`spec/api-contract.md` ← consolidated §6).
- [x] Architecture / stack / repos / file maps (`spec/architecture.md` ← consolidated §§2–3).
- [x] UI flow / routes / pages (`spec/ui-flow.md` ← consolidated §10).
- [x] Test strategy (`spec/test-strategy.md` ← consolidated §§11, 14).
- Evidence: `docs/SPEC_CONSOLIDATED.md` §§1–14 is the single build contract; `SPEC.md` frozen as v1 history.

## Stage 3 — Plan / Tasks (THIS FILE)

Staged build order (from consolidated §13); each task independently verifiable:

| # | Task | Spec section | Output | Verify |
|---|------|--------------|--------|--------|
| B1 | Scaffold Maven project (`pom.xml`, Java 21, Boot 3.2.5) | §2 | `backend/pom.xml` | `mvn -q compile` |
| B2 | Config: dev H2 file + test in-memory H2, CORS | §§8–9 | both `application.properties` | boot on `:8080`, `/h2-console` |
| B3 | Domain: `Ticket`, `TicketStatus.canTransitionTo()`, `Priority`, `Comment` | §§4–5 | `domain/*` | compile |
| B4 | Repositories: status filter + `q` search + pageable variants | §§4, 6 | `repository/*` | compile |
| B5 | Service: 8 ops + 422 enforcement + blank→null | §§5, 7 | `service/*` | compile |
| B6 | Web: 6 DTOs + 9 endpoints (dual-mode list) + handler | §§6–7, 9 | `web/*`, `web/dto/*` | compile + curl |
| B7 | Tests: 9 state-machine tests | §11 | `TicketStateMachineTest.java` | `mvn test` → 9 green |
| F8 | Frontend scaffold (`package.json`, `vite.config.js` :5174, `index.html`) | §10.1 | standalone repo root | `npm run dev` serves `:5174` |
| F9 | API layer (`api.js`: `toPage`, `parseError/handle`, 7 fns) | §10.3 | `src/api.js` | list/create via console |
| F10 | Router shell (`main.jsx`, `App.jsx`, 4 routes + fallback) | §10.2 | `src/main.jsx`, `src/App.jsx` | routes render |
| F11 | Pages (`ListPage` pagination, `Create/Detail/EditPage`, `ErrorBanner`) | §10.4–10.5 | `src/pages/*.jsx`, `styles.css` | `npm run build` clean + §14 walkthrough |
| V12 | Verification: tests + curls + restart persistence | §14 | terminal output, `backend/data/*.db` | all §14 steps pass |
| R13 | Review slices + Postman collection | §6 | 5 slices, `postman/*.json` | import + run in order |

## Stage 4 — Implementation (DONE)

- [x] B1–B6 backend built in dependency order (domain → repos → service → web).
- Evidence: `backend/src/main/...` (18 files); `docs/prompt-history.md` entries 2–3, 7–8.

## Stage 5 — Testing (DONE)

- [x] B7: 9 tests (3 valid 2xx, 4 invalid 422, 2 validation 400).
- [x] Frontend: `npm run build` clean + manual walkthrough (§14 steps 1–6).
- [x] Persistence: create → restart → still listed (`backend/data/ticketdb.mv.db`).
- Evidence: `backend/src/test/.../TicketStateMachineTest.java`; `spec/test-strategy.md`.

## Stage 6 — Review (DONE)

- [x] Spec review per `commands/review-spec.md` (consolidated-first, fragments mirror, `SPEC.md` untouched, history appended).
- [x] Code review per `commands/review-code.md` (state machine, PUT/status split, 400/404/422, dual-mode list, CORS, H2 isolation).
- [x] Test review per `commands/generate-tests.md` (exact 9 names, MockMvc shape, in-memory H2).
- Evidence: `docs/PROMPT_DEMO.md` stages (b)–(f); `.specstory/history/2026-09-26-*.md`.

## Stage 7 — Fix (DONE, logged)

- [x] Fix 1: Surefire `*IT` vs `*Test` skip → renamed + re-ran to 9 green.
- [x] Fix 2: duplicate Vite 5173/5174 + broken proxy → single `:5174` with `strictPort` + proxy.
- [x] Fix 3: nested frontend path + `server.port=8081` override → canonical path + `8080` restored.
- Evidence: `docs/AI_MISTAKES.md` (a)(b)(c); `docs/prompt-history.md` entries 7–8.
