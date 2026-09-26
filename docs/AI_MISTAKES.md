# AI Mistakes — Support Ticket Build

> At least 3 meaningful AI mistakes actually encountered in this build: what the AI suggested/did, why it was wrong, the evidence that caught it, the fix, and the lesson. New file only — `SPEC.md` / `docs/SPEC_CONSOLIDATED.md` untouched.

## (a) Surefire silently skipping `*IT` tests renamed to `*Test` — caught by a 0-tests green build

**What the AI suggested/did:**

- The AI generated the state-machine integration tests in a file named `TicketStateMachineIT.java` (failsafe-style `*IT` naming) while the build only ran `mvn test` (Surefire, no Failsafe plugin configured), then later renamed the file to `TicketStateMachineTest.java` without re-checking the run output.

**Why it was wrong:**

- Default Surefire includes only `*Test` / `*Tests` / `*TestCase` patterns. A `*IT.java` file is invisible to `mvn test`, so the build reported `Tests run: 0 — BUILD SUCCESS`, which looks green but proves nothing. Renaming alone was not the fix until the run actually showed 9 tests.

**Evidence:**

- `mvn test` output showing `Tests run: 0, Failures: 0, Errors: 0, Skipped: 0` despite the test file existing — a green build with zero tests is the red flag.

**Fix:**

- Keep the file at `backend/src/test/java/com/example/ticketai/TicketStateMachineTest.java` (Surefire-visible `*Test` naming), ensure no Failsafe-only setup is required, and re-run `cd backend && mvn test` until the output reads `Tests run: 9, Failures: 0, Errors: 0`.

**Lesson:**

- Never accept a green build at face value — always check the test count. Validate output (9 tests listed by name), don't blindly accept `BUILD SUCCESS`.

## (b) Duplicate Vite on 5173/5174 with a broken `/api` proxy on the stale instance (+ missing strictPort/host) — caught via curl content-type + ss

**What the AI suggested/did:**

- The AI started a new Vite dev server for the standalone frontend on `:5174` without killing the old legacy-frontend server still running on `:5173`, and the stale `:5173` instance had no (or a wrong) `/api` proxy. The AI also omitted `server.strictPort`/`server.host`, so port drift went unnoticed, and declared "frontend works" because *a* page loaded.

**Why it was wrong:**

- Two dev servers meant the browser/verification could hit the stale instance, where `/api/tickets` returned `index.html` (or a proxy 404) instead of JSON, while the fresh `:5174` instance was never actually exercised. Without `strictPort`, Vite silently moves to the next free port, so "port 5174" was an assumption, not a fact.

**Evidence:**

- `curl -i localhost:5174/api/tickets` (and the `:5173` equivalent) returned `Content-Type: text/html` with Vite HTML instead of `application/json`; `ss -tlnp` showed two Vite listeners (5173 + 5174), proving the duplicate.

**Fix:**

- Killed the stale process, kept exactly one dev server with `vite.config.js` containing `server: { port: 5174, strictPort: true, host: true, proxy: { '/api': 'http://localhost:8080' } }`, and re-verified with `curl -i localhost:5174/api/tickets` returning JSON + `ss -tlnp` showing a single `:5174` listener.

**Lesson:**

- When ports matter, assert them: check the listener table and the response content-type, not just "page loads". Validate output, don't blindly accept.

## (c) Frontend landing in nested `ticket_frontend/ticket_frontend_ai` vs requested path + `server.port 8081` override confusion — caught via ls + reading application.properties

**What the AI suggested/did:**

- Asked to scaffold the standalone frontend at a given path, the AI created it one level too deep (`ticket_frontend/ticket_frontend_ai/` containing *another* `ticket_frontend_ai/`), and in parallel left/added a `server.port=8081` override in `backend/src/main/resources/application.properties` (deviating from the SPEC default `8080`), then ran backend-dependent checks against the wrong URL/port combination.

**Why it was wrong:**

- The nested path broke every documented command (`cd ticket_frontend/ticket_frontend_ai && npm run dev` ran in the wrong directory or against a half-empty scaffold), and the `8081` override silently invalidated the whole contract: frontend proxy (`/api → :8080`), CORS origins, Postman collection, and all curl examples assume `:8080`.

**Evidence:**

- `ls ticket_frontend/ticket_frontend_ai` showed an unexpected nested directory instead of `package.json`/`vite.config.js` at the top; reading `backend/src/main/resources/application.properties` showed `server.port=8081` contradicting SPEC (`8080`).

**Fix:**

- Moved/collapsed the scaffold so `package.json`, `vite.config.js`, `index.html`, `src/` sit directly under `ticket_frontend/ticket_frontend_ai/` (verified with `ls`), and restored `server.port=8080` in `application.properties` (verified by reading the file), then re-ran backend + frontend + curl checks on the canonical ports.

**Lesson:**

- Paths and ports are load-bearing — always `ls` the created tree and `read` the config file after scaffolding. Validate output, don't blindly accept the AI's claim about where files landed.
