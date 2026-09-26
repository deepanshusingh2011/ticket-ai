# Architecture

> Derived from `docs/SPEC_CONSOLIDATED.md` (single source, §§2–3, 8–9). Focused: stack, repos, runtime shape. For endpoints see `api-contract.md`; for tables see `data-model.md`.

## Stack (pinned)

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.2.x (parent `3.2.5`), Spring Web + Data JPA + Validation, H2 (runtime), spring-boot-starter-test |
| Backend build | Maven (`mvn`), `backend/pom.xml` with `<java.version>21</java.version>` |
| Backend port | `8080` (`server.port=8080`) |
| Frontend | Vite 5 (`^5.4.8`), React 18 (`^18.3.1`), `react-dom` 18, `react-router-dom@6` (`^6.30.6`), `@vitejs/plugin-react` 4 |
| Frontend build | npm (`npm install`, `npm run dev`, `npm run build`) |
| Frontend port | `5174` (dev server + preview) |
| DB | H2 file mode (dev) / H2 in-memory (tests) |
| Data format | JSON everywhere; UTF-8 |

Prerequisites: Java 21, Maven 3.6+, Node 18+.

## Repos

Two repos/siblings. Backend lives in the main repo; the frontend is a **standalone repo**. The legacy `frontend/` copy inside the backend repo, if present, is frozen — do not extend it.

**Repo A — backend** (`ticket-ai`, e.g. branch `backup/main-work-20260926`):

```
backend/pom.xml
backend/src/main/java/com/example/ticketai/
  TicketAiApplication.java
  domain/Ticket.java
  domain/TicketStatus.java        # enum + canTransitionTo()
  domain/Priority.java            # LOW | MEDIUM | HIGH | URGENT
  domain/Comment.java
  repository/TicketRepository.java
  repository/CommentRepository.java
  service/TicketService.java
  service/NotFoundException.java
  service/InvalidStatusTransitionException.java
  web/TicketController.java
  web/GlobalExceptionHandler.java
  web/dto/TicketRequest.java
  web/dto/TicketUpdateRequest.java
  web/dto/StatusChangeRequest.java
  web/dto/CommentRequest.java
  web/dto/TicketResponse.java
  web/dto/CommentResponse.java
backend/src/main/resources/application.properties
backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java
backend/src/test/resources/application.properties   # in-memory H2 for tests
docs/SPEC_CONSOLIDATED.md
postman/TicketAI.postman_collection.json
```

**Repo B — frontend standalone** (`ticket_frontend/ticket_frontend_ai`):

```
package.json            # name ticket-ai-frontend, scripts dev/build/preview
vite.config.js          # port 5174, proxy /api -> http://localhost:8080
index.html
src/main.jsx            # React root + <BrowserRouter>
src/App.jsx             # header/nav + <Routes>
src/api.js              # fetch wrapper + STATUSES/PRIORITIES/PAGE_SIZES/allowedNext()
src/styles.css
src/pages/ListPage.jsx
src/pages/CreatePage.jsx
src/pages/DetailPage.jsx
src/pages/EditPage.jsx
```

## Runtime & config

- Backend serves `http://localhost:8080`; H2 console at `/h2-console`.
- Frontend dev server on `:5174` proxies `/api → http://localhost:8080` (see `vite.config.js`), so relative `/api/...` fetches work without CORS in dev.
- Backend `@CrossOrigin` on `TicketController` allows `http://localhost:5174`, `http://localhost:5173` (legacy), `http://localhost:3000`.
- `vite.config.js`: `server: { port: 5174, proxy: { '/api': 'http://localhost:8080' } }`.
- `src/main.jsx`: `createRoot(...).render(<StrictMode><BrowserRouter><App/></BrowserRouter></StrictMode>)`.

## Build order

Backend first (scaffold → config → domain → repositories → service → web → tests), then frontend (scaffold → api layer → router shell → pages). Each step independently verifiable; see consolidated spec §13.
