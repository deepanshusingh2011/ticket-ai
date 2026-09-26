# Testing Rules — TicketAI Backend

Source of truth: `docs/SPEC_CONSOLIDATED.md` §11 + `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java`.

## The 9 state-machine tests (exact set — keep at 9, keep names)

Valid (expect 2xx):

1. `validLifecycle_open_to_closed` — `OPEN→IN_PROGRESS→RESOLVED→CLOSED` full walk succeeds.
2. `valid_open_to_cancelled` — `OPEN→CANCELLED` succeeds.
3. `valid_inProgress_to_cancelled` — create, `→IN_PROGRESS`, then `→CANCELLED` succeeds.

Invalid (expect **422**):

4. `invalid_open_to_resolved_isRejected` — `OPEN→RESOLVED` rejected.
5. `invalid_closed_to_open_isRejected` — terminal `CLOSED→OPEN` rejected.
6. `invalid_resolved_to_open_isRejected` — `RESOLVED→OPEN` rejected.
7. `invalid_cancelled_to_open_isRejected` — terminal `CANCELLED→OPEN` rejected.

Validation (expect **400**):

8. `validation_create_requiresTitleAndDescription` — empty title/description → 400 with
   `$.fields.title` and `$.fields.description`.
9. `comment_requiresBody` — empty comment body → 400.

## MockMvc patterns (copy this shape)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketStateMachineTest {
    @Autowired private MockMvc mvc;
    @Autowired private TicketRepository tickets;

    private Long createOpenTicket() {
        Ticket ticket = tickets.saveAndFlush(
                new Ticket("Login broken", "Cannot log in", Priority.HIGH, "ada"));
        return ticket.getId();
    }

    private void moveTo(Long id, String status) throws Exception {
        mvc.perform(patch("/api/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }
}
```

- Seed via `TicketRepository.saveAndFlush(...)`, drive transitions over HTTP via MockMvc
  (`patch`, `post`), assert with `status()` + `jsonPath()`.
- Invalid transitions: `.andExpect(status().isUnprocessableEntity())` (422, not 400/200).
- Validation failures: `.andExpect(status().isBadRequest())` + `jsonPath("$.fields.<field>").exists()`.
- `@Transactional` on the class rolls back each test — no manual cleanup, no ordering dependence.

## In-memory H2 for tests

- `backend/src/test/resources/application.properties` overrides the file DB with
  `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1` + `ddl-auto=create-drop`.
- Tests must never read/write `backend/data/`. If a test needs the H2 console or file URL,
  it is testing the wrong thing.

## Running & verifying

```bash
cd backend && mvn test            # expect 9 green
cd backend && mvn -q compile      # after domain/web changes
```

Manual API check after green tests (backend on `:8080`):

```bash
# valid walk -> 200 each; OPEN->RESOLVED on fresh ticket -> 422
curl -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'
curl -i -X PATCH localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' -d '{"status":"RESOLVED"}'
# empty title/description -> 400 with fields; empty comment body -> 400
curl -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' -d '{"title":"","description":""}'
```

## Adding tests

- New transition coverage → extend the table in `TicketStatus` first, then add a test named
  `invalid_<from>_to_<to>_isRejected` / `valid_<path>` following the existing style.
- New validation rule → add a `validation_*` test asserting 400 + the `$.fields.<field>` key.
- Do not add `@MockBean` service tests as substitutes — state-machine tests stay full-stack
  (`@SpringBootTest` + MockMvc + real H2).
