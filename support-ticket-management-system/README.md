# Support Ticket Management System

Create, track and resolve support tickets. Tickets move through a backend-enforced workflow:

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

Every other change (for example `CLOSED → OPEN`, `RESOLVED → OPEN`, `CANCELLED → OPEN`) is rejected
with `409 INVALID_STATUS_TRANSITION`.

| Part     | Stack                                                                    |
| -------- | ------------------------------------------------------------------------ |
| Backend  | Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, H2 (file mode), springdoc |
| Frontend | Next.js (App Router), React, TypeScript, CSS Modules, Vitest             |

Design documents (spec, plan, data model, API contract, tasks) are in
[`specs/003-ticket-management-workflow/`](specs/003-ticket-management-workflow/).

## Prerequisites

- JDK 21 (`java -version` must show 21; set `JAVA_HOME` if another JDK is first on `PATH`)
- Node.js 20.19 or later and npm
- Maven is provided by `backend/mvnw`

## Run

```bash
# Terminal 1: backend on http://localhost:8080
export TICKETS_DB_PASSWORD='<choose-a-local-password>'   # never commit this
cd backend && ./mvnw spring-boot:run

# Terminal 2: frontend on http://localhost:3000
cd frontend && npm install
BACKEND_URL=http://localhost:8080 npm run dev
```

Data is stored in `backend/data/` (git-ignored) and survives restarts. To use another backend
address or port, change `BACKEND_URL`. It is read when the frontend starts in development and when it
is **built** for production (`BACKEND_URL=... npm run build`), because Next.js bakes rewrites into the
build.

The browser talks only to the frontend (`/api/v1/*` is proxied to the backend), so no CORS
configuration is needed. API documentation: <http://localhost:8080/swagger-ui.html>.

## Test

```bash
cd backend  && ./mvnw verify                          # unit, repository, MockMvc and integration tests
cd frontend && npm run lint && npm test && npm run build
```

## Performance check

```bash
cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=perf
```

Starts the backend with 10,000 seeded tickets in a throw-away in-memory database (the real data
folder is untouched). Add `-Dspring-boot.run.arguments=--server.port=<port>` if 8080 is in use.

## Notes

- There is no login: any user of the app can see and change every ticket. It is intended for local
  use; add authentication before exposing it on a network.
- Error responses follow one shape (`code`, `message`, `fieldErrors`, ...) described in
  `specs/003-ticket-management-workflow/contracts/openapi.yaml`.
