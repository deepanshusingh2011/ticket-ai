# 2026-09-26 — Spec-first

> Specstory-style record of the key prompts. Extension-mirror file (see also `docs/prompt-history.md` entries 1–2). `SPEC.md` / `docs/SPEC_CONSOLIDATED.md` not modified here.

## Prompt 1 — spec before code

**User:**

> Create a minimal support-ticket tracker spec first, before any code. Stack: Java 21 + Spring Boot 3.2.x + H2 file DB on :8080, Vite + React on the frontend. Requirements F1–F11, domain model, backend-enforced state machine, API design, validation rules, UI/UX, H2 persistence, test strategy, acceptance criteria, file map. Save as SPEC.md, the source of truth.

**AI output:**

- `SPEC.md` (§§1–11) + `README.md`.
- No code yet — spec reviewed and approved first.

**Verification:** read SPEC end-to-end; F1–F11, state table, and file map complete.

## Prompt 2 — backend scaffold

**User:**

> Scaffold the backend from SPEC §§2–5, smallest diffs first: domain, repositories, service with changeStatus enforcement, web layer with DTOs, config with H2 file DB + CORS.

**AI output:**

- `backend/pom.xml`, `domain/{Ticket,TicketStatus,Priority,Comment}.java`, `repository/*`, `service/*`, `web/* + dto/*`, `application.properties`.

**Verification:** `cd backend && mvn -q compile` green; `mvn spring-boot:run` on `:8080`.
