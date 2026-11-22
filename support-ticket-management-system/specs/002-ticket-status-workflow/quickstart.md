# Quickstart: Validate the Ticket Status Workflow

**Feature**: `002-ticket-status-workflow` | **Date**: 2026-11-22

This is a run-and-validate guide. It becomes runnable once the backend and frontend described in
[plan.md](./plan.md) are implemented. Contract details: [contracts/openapi.yaml](./contracts/openapi.yaml).
Status rules and the full 20-pair table: [data-model.md](./data-model.md).

## Prerequisites

- **JDK 21** (the machine currently has JDK 17; install 21 and set `JAVA_HOME`). Check:
  `java -version` shows `21`.
- **Node.js 20 LTS or later** and npm (Node 24 is installed). Check: `node -v`.
- Maven is provided by the wrapper (`backend/mvnw`); the installed Maven 3.6.3 is not used.
- `curl` and `jq` for the API checks.
- Be on branch `002-ticket-status-workflow`, not `main`.

## Setup

```bash
# Backend (terminal 1)
export TICKETS_DB_PASSWORD='<choose-a-local-password>'   # do not commit; use any local value
cd backend && ./mvnw spring-boot:run          # http://localhost:8080

# Frontend (terminal 2)
cd frontend && npm install
BACKEND_URL=http://localhost:8080 npm run dev # http://localhost:3000
```

## Automated checks

```bash
cd backend  && ./mvnw verify      # unit, repository, MockMvc and integration tests on in-memory H2
cd frontend && npm test           # Vitest: status selector and error display
```

Expected: all green. `TicketStatusTest` reports 20 pair cases (5 accepted, 15 rejected).

## Scenario 1: Normal lifecycle (User Story 1, SC-001)

```bash
curl -s -X POST localhost:8080/api/v1/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Printer down","description":"Floor 2 printer offline","priority":"HIGH"}' | jq '.id,.status'
# expect status "OPEN"; note the id as $ID
for s in IN_PROGRESS RESOLVED CLOSED; do
  curl -s -X PATCH localhost:8080/api/v1/tickets/$ID/status -H 'Content-Type: application/json' \
    -d "{\"status\":\"$s\"}" | jq -r '.status,.allowedNextStatuses'
done
```

Expected: each call returns `200`; statuses go `IN_PROGRESS`, `RESOLVED`, `CLOSED`; after `CLOSED`
`allowedNextStatuses` is `[]`; `updatedAt` changes on each step.

## Scenario 2: Invalid transitions are rejected (User Story 2, SC-002 to SC-004)

```bash
# $ID is CLOSED from scenario 1
curl -s -i -X PATCH localhost:8080/api/v1/tickets/$ID/status -H 'Content-Type: application/json' \
  -d '{"status":"OPEN"}'
```

Expected: `HTTP/1.1 409`, body `code = INVALID_STATUS_TRANSITION`, `currentStatus = CLOSED`,
`requestedStatus = OPEN`, `allowedNextStatuses = []`, and a plain-language `message`. A following
`GET` shows the ticket unchanged, including `updatedAt`.

Also verify (each must be `409`): new ticket `OPEN → RESOLVED`; `IN_PROGRESS → OPEN`;
`RESOLVED → OPEN`; `RESOLVED → CANCELLED`; `CANCELLED → OPEN`.

Unknown value:

```bash
curl -s -i -X PATCH localhost:8080/api/v1/tickets/$ID/status -H 'Content-Type: application/json' \
  -d '{"status":"DONE"}'
```

Expected: `400`, `code = VALIDATION_ERROR`, a `fieldErrors` entry for `status` listing the five valid
values (not an invalid-transition message).

Same status is not a transition: `PATCH` a fresh `OPEN` ticket with `{"status":"OPEN"}` returns `200`.

## Scenario 3: Cancellation (User Story 3)

Create two tickets. Cancel one while `OPEN` and move the other to `IN_PROGRESS` then cancel it.
Expected: both `CANCELLED`, and `GET /api/v1/tickets?status=CANCELLED` lists both. Cancelling a
`RESOLVED`, `CLOSED` or `CANCELLED` ticket returns `409`.

## Scenario 4: Create ignores a supplied status (FR-002)

```bash
curl -s -X POST localhost:8080/api/v1/tickets -H 'Content-Type: application/json' \
  -d '{"title":"T","description":"D","status":"CLOSED"}' | jq -r '.status'
```

Expected: `OPEN`.

## Scenario 5: Simultaneous changes (FR-012)

Send two `PATCH` requests at the same moment to the same `OPEN` ticket, one `IN_PROGRESS` and one
`CANCELLED`, for example with `&` in a shell. Expected: exactly one request wins; the other either
succeeds legitimately against the new status or returns `409` naming the new current status. The
ticket never ends in an impossible state. The automated concurrency test covers this repeatedly.

## Scenario 6: UI behavior (User Story 4, SC-006)

Open `http://localhost:3000/tickets` and a ticket detail page for each status.

| Ticket status | Status choices shown          |
|---------------|-------------------------------|
| OPEN          | In Progress, Cancelled        |
| IN_PROGRESS   | Resolved, Cancelled           |
| RESOLVED      | Closed                        |
| CLOSED        | none (shown as final)         |
| CANCELLED     | none (shown as final)         |

Then change a ticket's status in another tab and submit a now-invalid change in the first tab.
Expected: a plain-language message from the server and a refresh to the real current status. Resize
the window to 320 px wide; the list becomes stacked cards with no horizontal scrolling. The status
filter includes Cancelled.

## Scenario 7: Persistence (SC-006 of spec 001, FR-011)

Stop and restart the backend. Expected: tickets and their statuses are still present (H2 file in
`backend/data/`).

## Performance smoke check

Seed 10,000 tickets (the seed script is added during implementation as a test-profile
`CommandLineRunner`, not part of production code), then time the endpoints:

```bash
for url in "/api/v1/tickets?size=20" "/api/v1/tickets?status=OPEN&q=printer" "/api/v1/tickets/$ID"; do
  curl -s -o /dev/null -w "$url %{time_total}s\n" "localhost:8080$url"
done
```

Expected: warm responses under 100 ms. In the browser, Lighthouse on `/tickets` should report first
contentful paint under 1.5 s and the list page under 100 KB gzipped JavaScript.
