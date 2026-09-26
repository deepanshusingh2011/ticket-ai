# 2026-09-26 — Backend scaffold + verification

> Specstory-style record of the key prompts. Extension-mirror file (see also `docs/prompt-history.md` entries 3–6). `SPEC.md` / `docs/SPEC_CONSOLIDATED.md` not modified here.

## Prompt 1 — frontend scaffold (legacy SPA)

**User:**

> Scaffold the frontend from SPEC §6: single-page app with no router. App.jsx (create form, list + search/filter, detail, status buttons, edit form, comments, error banner), api.js (fetch wrapper + allowedNext), main.jsx, styles.css, vite proxy /api → :8080.

**AI output:**

- `frontend/package.json`, `vite.config.js`, `index.html`, `src/{main.jsx,App.jsx,api.js,styles.css}`.

**Verification:** `npm install && npm run dev`; manual create → list → detail → comment.

## Prompt 2 — verification (9 tests + persistence)

**User:**

> Write 9 state-machine integration tests (3 valid, 4 invalid → 422, 2 validation → 400). Run mvn test (9 green). Verify persistence across restart (H2 file in backend/data/).

**AI output:**

- `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java`, `src/test/resources/application.properties`.

**Verification:** `cd backend && mvn test` → 9 green; create → restart → still listed. (This step caught the Surefire `*IT` skip — see `docs/AI_MISTAKES.md`.)

## Prompt 3 — PR split + Postman

**User:**

> Split work into 5 reviewable slices (entities → repos → service → web → frontend) on branch backup/main-work-20260926. Verify all endpoints with curl and save postman/TicketAI.postman_collection.json.

**AI output:**

- Branch `backup/main-work-20260926`; `postman/TicketAI.postman_collection.json`.

**Verification:** each curl returns expected 201/200/422/204; collection imports cleanly.
