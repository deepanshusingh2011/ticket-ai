# /generate-tests — TicketAI Test Generation

Checklist + prompt for (re)generating `TicketStateMachineTest` (exactly 9 tests).

## Checklist

- [ ] Class annotations: `@SpringBootTest @AutoConfigureMockMvc @Transactional`; inject
      `MockMvc` + `TicketRepository`; helpers `createOpenTicket()` (saveAndFlush) and
      `moveTo(id, status)` (PATCH + `isOk` + `jsonPath("$.status")`) per existing file.
- [ ] 3 valid: full walk `OPEN→IN_PROGRESS→RESOLVED→CLOSED`; `OPEN→CANCELLED`;
      `OPEN→IN_PROGRESS→CANCELLED` (expect 2xx).
- [ ] 4 invalid → `isUnprocessableEntity()` (422): `OPEN→RESOLVED`, `CLOSED→OPEN`,
      `RESOLVED→OPEN`, `CANCELLED→OPEN`.
- [ ] 2 validation → `isBadRequest()` (400): empty title+description asserting
      `$.fields.title` + `$.fields.description`; empty comment body.
- [ ] In-memory H2 only (`src/test/resources/application.properties`); no `backend/data/` access;
      run `cd backend && mvn test` → 9 green.

## Prompt

```text
(Regenerate/extend) backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java
following rules/testing.md. Keep the 9 tests above with exact names and MockMvc patterns;
seed via TicketRepository.saveAndFlush, drive over HTTP, assert status()+jsonPath().
Use in-memory H2 only. After writing, run `cd backend && mvn test` and report 9 green
(or paste failures). Also give curl equivalents for one valid walk, one 422, and one 400.
```
