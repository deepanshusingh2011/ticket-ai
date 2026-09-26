# 2026-09-26 — Pagination + pages + consolidated spec

> Specstory-style record of the key prompts. Extension-mirror file (see also `docs/prompt-history.md` entries 7–10). `SPEC.md` / `docs/SPEC_CONSOLIDATED.md` not modified here.

## Prompt 1 — repo split (standalone frontend :5174)

**User:**

> Move the UI to standalone repo ticket_frontend/ticket_frontend_ai on :5174 with /api proxy to :8080. Freeze legacy frontend/. Fix duplicate-Vite 5173/5174 proxy issue and nested-path + server.port 8081 confusion.

**AI output:**

- `ticket_frontend/ticket_frontend_ai/{package.json,vite.config.js,index.html,src/*}`.

**Verification:** `ls` confirms exact path; `server.port=8080`; `curl -i localhost:5174/api/tickets` returns JSON; single Vite listener via `ss`. (Mistakes + fixes: see `docs/AI_MISTAKES.md`.)

## Prompt 2 — pages + pagination

**User:**

> Upgrade to react-router-dom@6 (/, /create, /tickets/:id, /tickets/:id/edit). Split into ListPage (filters + table + pagination 5/10/20, Prev/numbered/Next, counts), CreatePage, DetailPage (five status buttons + allowedNext hint + comments + delete), EditPage, ErrorBanner. Backend: dual-mode list (any page/size/sort → envelope {content, page, size, totalElements, totalPages}; none → bare array). Add toPage() normalization.

**AI output:**

- Backend dual-mode list + pageable queries; frontend `api.js`, `App.jsx` routes, `pages/*.jsx`.

**Verification:** paged curl returns envelope, bare curl returns array; page-size/Prev/numbered/Next update counts; `npm run build` clean.

## Prompt 3 — consolidated spec + demo guide

**User:**

> Consolidate into docs/SPEC_CONSOLIDATED.md (single rebuild contract: F1–F13, stack/ports, file maps, domain, state machine, API + envelope + curls, validation, persistence, CORS, frontend routes + pagination UX, 9 tests, acceptance, build order, verification §14). Do not modify SPEC.md. Then add the demo walkthrough.

**AI output:**

- `docs/SPEC_CONSOLIDATED.md` + `docs/{BACKEND,FRONTEND,CHAT_HISTORY}.md`.

**Verification:** consolidated spec diffed against SPEC + tree; full demo script passes (9 green, persistence, 422 banners, delete → 204 → 404).
