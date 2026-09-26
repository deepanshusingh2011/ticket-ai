# Data Model

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §§4, 7–8). Focused: entities, validation, persistence.

## Entities

```
Ticket (table: tickets)
  id:          Long, PK, auto-increment (GenerationType.IDENTITY)
  title:       String, required, 1..200
  description: String, required, 1..4000 (TEXT/LOB-safe length)
  status:      TicketStatus enum (STRING mapping), required, default OPEN
  priority:    Priority enum (STRING mapping), required, default MEDIUM
  assignee:    String | null, 0..100; blank/whitespace-only -> null on write
  createdAt:   Instant, immutable (set on create, never updated)
  updatedAt:   Instant, set on create, refreshed on every mutation
  comments:    List<Comment>, @OneToMany(mappedBy ticket, cascade ALL, orphanRemoval true)

Comment (table: comments)
  id:        Long, PK, auto-increment
  ticket:    ManyToOne -> Ticket, FK ticket_id, required (nullable=false)
  body:      String, required, 1..2000
  author:    String | null, 0..100; blank -> null
  createdAt: Instant, immutable

TicketStatus: OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED
Priority:     LOW | MEDIUM | HIGH | URGENT
```

## Implementation notes

- `TicketStatus.canTransitionTo(target)` returns true exactly for: `OPEN→IN_PROGRESS`, `OPEN→CANCELLED`, `IN_PROGRESS→RESOLVED`, `IN_PROGRESS→CANCELLED`, `RESOLVED→CLOSED`. Same-status is a service-level no-op.
- Search: keyword `q` matches `title` OR `description`, case-insensitive partial (`LIKE %q%`). Status filter is exact enum match. Both combinable, both optional.
- List ordering default: `id DESC` (newest first). Sort param (paged mode) overrides.
- `TicketResponse`: `id, title, description, status, priority, assignee, createdAt, updatedAt, comments[]` (each: `id, body, author, createdAt`). Timestamps are ISO-8601 instants.
- PUT must NOT change status — status changes only via PATCH.

## Validation rules

| Field | Rule | Failure |
|-------|------|---------|
| `ticket.title` | required, non-blank, 1..200 | 400 `fields.title` |
| `ticket.description` | required, non-blank, 1..4000 | 400 `fields.description` |
| `ticket.priority` | optional enum, default MEDIUM; unknown rejected | 400 on unknown value |
| `ticket.assignee` | optional, ≤100 chars; blank/whitespace → null | 400 `fields.assignee` |
| status body `.status` | required enum | 400 if missing/unknown; 422 if illegal transition |
| `comment.body` | required, non-blank, 1..2000 | 400 `fields.body` |
| `comment.author` | optional, ≤100 chars; blank → null | 400 `fields.author` |
| path `id` | must exist | 404 `Ticket {id} not found` |

Backend uses Bean Validation (`@Valid` on `@RequestBody` DTOs) + `GlobalExceptionHandler`: validation → 400+`fields`, `NotFoundException` → 404, `InvalidStatusTransitionException` → 422, `IllegalArgumentException` (bad enum) → 400.

## Persistence & config

`backend/src/main/resources/application.properties` (dev source of truth):

```properties
server.port=8080

# H2 file-based persistence so data survives restarts (stored under ./data/
# relative to the backend/ working directory)
spring.datasource.url=jdbc:h2:file:./data/ticketdb;AUTO_SERVER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- Data file lands in `backend/data/` (`ticketdb.mv.db`). `ddl-auto=update`; data survives restarts. H2 console at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/ticketdb`, user `sa`, empty password — dev only).
- Tests MUST NOT touch the file DB: `backend/src/test/resources/application.properties` overrides with in-memory H2 (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`, `ddl-auto=create-drop`).
