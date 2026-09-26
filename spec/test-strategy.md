# Test Strategy

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §§11, 14). Focused: what to test and how to verify.

## Backend tests (9 state-machine tests)

File: `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java` (`@SpringBootTest` + MockMvc, `@Transactional`, in-memory H2 via `backend/src/test/resources/application.properties`).

Valid (expect 2xx):

1. `validLifecycle_open_to_closed` — `OPEN→IN_PROGRESS→RESOLVED→CLOSED` full walk succeeds.
2. `valid_open_to_cancelled` — `OPEN→CANCELLED` succeeds.
3. `valid_inProgress_to_cancelled` — create, move to `IN_PROGRESS`, then `→CANCELLED` succeeds.

Invalid (expect **422**):

4. `invalid_open_to_resolved_isRejected` — `OPEN→RESOLVED` rejected.
5. `invalid_closed_to_open_isRejected` — terminal `CLOSED→OPEN` rejected.
6. `invalid_resolved_to_open_isRejected` — `RESOLVED→OPEN` rejected.
7. `invalid_cancelled_to_open_isRejected` — terminal `CANCELLED→OPEN` rejected.

Validation (expect **400**):

8. `validation_create_requiresTitleAndDescription` — empty title/description rejected.
9. `comment_requiresBody` — empty comment body rejected.

Run:

```bash
cd backend
mvn test   # → 9 green
```

Test isolation: tests MUST NOT touch the file DB — the test `application.properties` overrides with in-memory H2 (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`, `ddl-auto=create-drop`).

## Frontend checks

- `npm run build` must be clean.
- Manual walkthrough (backend must be up first):
  1. Create ticket → appears in list → open `/tickets/{id}` → edit fields → add comments.
  2. Walk `OPEN→IN_PROGRESS→RESOLVED→CLOSED` (all succeed); attempt `CLOSED→OPEN` (422 banner). Try `OPEN→RESOLVED` on a fresh ticket (422 banner).
  3. Search keyword + status filter, combined, then Reset; paginate (page sizes 5/10/20, Prev/numbered/Next, counts update).
  4. Restart backend → tickets persist (H2 file in `backend/data/`).
  5. Delete a ticket → 204 → redirects to `/`; direct GET on it → 404 banner / `Ticket not found`.
  6. Debug aids: browser devtools network tab for 400/422 bodies; H2 console (`jdbc:h2:file:./data/ticketdb`, user `sa`, empty password).

## Full verification commands

```bash
# Backend tests (9 green)
cd backend
mvn test

# Backend run
mvn spring-boot:run
# API at http://localhost:8080, H2 console at http://localhost:8080/h2-console

# Frontend run (backend must be up first)
cd ticket_frontend/ticket_frontend_ai
npm install
npm run dev      # UI at http://localhost:5174
npm run build    # must be clean
```
