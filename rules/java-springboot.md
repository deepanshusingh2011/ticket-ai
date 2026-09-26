# Java / Spring Boot Rules — TicketAI Backend

Source of truth: `docs/SPEC_CONSOLIDATED.md` (§2, §4, §5, §7, §8, §9) + `backend/src/main/java/com/example/ticketai/`.
Derived from `TicketStatus`, `TicketService`, `TicketController`, `GlobalExceptionHandler`.

## Stack (pin these)

- Java 21 (`<java.version>21</java.version>`), Spring Boot parent `3.2.5`.
- Starters: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-validation`, H2 (`runtime`), `spring-boot-starter-test` (test scope).
- Build with Maven from `backend/`: `mvn -q compile`, `mvn test`, `mvn spring-boot:run`.
- Server port `8080` (`server.port=8080`). JSON everywhere, UTF-8.

## Domain conventions

- Package root: `com.example.ticketai` → `domain/`, `repository/`, `service/`, `web/`, `web/dto/`.
- Entities: `Ticket` (table `tickets`, `IDENTITY` id), `Comment` (table `comments`, `ManyToOne` → ticket,
  `nullable=false`). `@OneToMany(mappedBy=ticket, cascade=ALL, orphanRemoval=true)` on `Ticket.comments`.
- Enums as `STRING`: `TicketStatus {OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED}`,
  `Priority {LOW, MEDIUM, HIGH, URGENT}`. Defaults: status `OPEN`, priority `MEDIUM`.
- Timestamps: `Instant createdAt` (immutable, set once), `Instant updatedAt` (set on create,
  `touch()` on every mutation). Serialize as ISO-8601.
- Normalization (service layer, not controller): blank/whitespace-only `assignee`/`author` → `null`
  via `normalizeToNull()`; trim `title`/`description`/`body` on write. Max lengths enforced by
  Bean Validation on DTOs.

## State-machine enforcement (backend is authoritative)

- `TicketStatus.canTransitionTo(target)` is the ONLY place the transition table lives:
  `OPEN→IN_PROGRESS|CANCELLED`, `IN_PROGRESS→RESOLVED|CANCELLED`, `RESOLVED→CLOSED`,
  `CLOSED/CANCELLED→none`. Implemented as a `switch` expression.
- `TicketService.changeStatus(id, target)`: load or 404 → if `current == target` return as-is
  (no-op, HTTP 200) → if `!current.canTransitionTo(target)` throw
  `InvalidStatusTransitionException` naming the transition + allowed targets.
- Never enforce transitions in the controller or frontend. Frontend may hint via `allowedNext()`
  but all five status buttons stay clickable so the backend 422 is observable.
- `PUT /api/tickets/{id}` must NEVER change status — status changes only via
  `PATCH /api/tickets/{id}/status`.

## Validation (Bean Validation + handler)

- Put `@Valid` on every `@RequestBody` DTO. Rules (§7): title 1..200 non-blank,
  description 1..4000 non-blank, priority optional enum (default MEDIUM), assignee ≤100 optional,
  status body required enum, comment body 1..2000 non-blank, comment author ≤100 optional.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping — do not change shapes:
  - `MethodArgumentNotValidException` → 400 `{"timestamp","status":400,"error":"Validation failed","fields":{...}}`
  - `NotFoundException` → 404 `{"timestamp","status":404,"error":"Ticket {id} not found"}`
  - `InvalidStatusTransitionException` → 422 `{"timestamp","status":422,"error":"Invalid status transition from X to Y. Allowed: ..."}`
  - `IllegalArgumentException` (unknown enum value, e.g. `"priority":"P9"`) → 400, NOT 422.

## Persistence: H2 file vs test in-memory

- Dev (`backend/src/main/resources/application.properties`): file DB
  `jdbc:h2:file:./data/ticketdb;AUTO_SERVER=TRUE`, `sa`/empty, `ddl-auto=update`,
  console enabled at `/h2-console`. Data file lands in `backend/data/` and survives restarts.
- Tests (`backend/src/test/resources/application.properties`): MUST override with in-memory H2
  (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`, `ddl-auto=create-drop`). Tests must never touch `./data/`.
- Never commit `backend/data/*.db` or secrets. `sa`/empty password is dev-only.

## CORS

- Per-controller `@CrossOrigin` on `TicketController` (not global config):
  `http://localhost:5174` (standalone frontend), `http://localhost:5173` (legacy),
  `http://localhost:3000`. Keep all three.
- Frontend dev server additionally proxies `/api → http://localhost:8080`, so relative fetch
  calls work without CORS in dev. Do not remove the proxy instead of fixing CORS.

## Anti-patterns (reject in review)

- Transition logic outside `TicketStatus`/`TicketService`; status in PUT DTO; 422 vs 400 confusion
  (unknown enum → 400, illegal transition → 422); same-status PATCH treated as error;
  global `*` CORS; file-DB URL in test resources; committing `backend/data/`.
