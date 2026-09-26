# Requirements

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §1 + §12). This file is focused: what the system must do. For how, see `architecture.md`, `data-model.md`, `api-contract.md`, `state-machine.md`, `ui-flow.md`, `test-strategy.md`.

## Goal

Minimal support-ticket tracker for small co-located teams: create tickets, triage through a controlled lifecycle, edit fields, add discussion comments, search by keyword, filter by status, paginate the list. Single-team local tool.

## Functional requirements

| ID | Requirement | Acceptance hint |
|----|-------------|-----------------|
| F1 | Create ticket (title, description, priority, assignee) | POST → 201, appears in list |
| F2 | List tickets (paginated) | GET envelope with page/size controls |
| F3 | View ticket details incl. comments | GET by id shows fields + comments |
| F4 | Update title / description / priority / assignee | PUT → 200; status NOT changeable here |
| F5 | Change status only via allowed transitions (backend-enforced) | PATCH valid → 200; invalid → 422 banner |
| F6 | Add comments to a ticket | POST comment → 201, listed under ticket |
| F7 | Search by keyword (title + description, case-insensitive, partial) | `q` matches `%q%` either field |
| F8 | Filter by status | `status` exact enum match |
| F9 | Combine status filter + keyword search | `status` + `q` together |
| F10 | Persist data across restarts (H2 file mode) | Create → restart → still listed |
| F11 | Backend validation with meaningful UI errors | 400 `fields` joined in banner, no silent failures |
| F12 | Delete ticket | DELETE → 204, redirects to `/` |
| F13 | Paginated list with page/size controls, page-size selector, Prev/Next + numbered buttons, result counts | Label `Showing {from}–{to} of {total} · page {n} of {m}` |

## Explicit non-goals (v1)

Authentication/authorization, roles, file attachments, SLA timers, email notifications.

## Acceptance criteria (checklist)

- [ ] F1–F13 all work from the UI.
- [ ] Valid transitions succeed end-to-end; invalid ones show a visible 422 banner.
- [ ] Pagination: page/size/sort params return the envelope; bare list (no params) still returns an array.
- [ ] Data survives backend restart (H2 file in `backend/data/`).
- [ ] `mvn test` green (9 tests); `npm run build` clean.
- [ ] Ports exactly `8080` (API) / `5174` (UI); CORS allows `5174/5173/3000`.
- [ ] No secrets committed; H2 uses local `sa`/empty password (dev only).
- [ ] Routes `/`, `/create`, `/tickets/:id`, `/tickets/:id/edit` all resolve (fallback `*` → list).
