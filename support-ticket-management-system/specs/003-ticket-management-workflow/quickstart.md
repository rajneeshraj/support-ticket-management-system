# Quickstart: Validate Ticket Management with Status Workflow

**Feature**: `003-ticket-management-workflow` | **Date**: 2026-11-22

This is a run-and-validate guide. It becomes runnable once the backend and frontend described in
[plan.md](./plan.md) are implemented. Contract: [contracts/openapi.yaml](./contracts/openapi.yaml).
Status rules and the 20-pair table: [data-model.md](./data-model.md).

## Prerequisites

- **JDK 21** (the machine currently has JDK 17; install 21 and set `JAVA_HOME`). Check:
  `java -version` shows `21`.
- **Node.js 20 LTS or later** and npm (24 is installed). Check: `node -v`.
- Maven comes from the wrapper (`backend/mvnw`); the installed Maven 3.6.3 is not used.
- `curl` and `jq` for the API checks.
- Be on branch `003-ticket-management-workflow`, not `main`.

## Setup

```bash
# Backend (terminal 1)
export TICKETS_DB_PASSWORD='<choose-a-local-password>'   # do not commit; any local value
cd backend && ./mvnw spring-boot:run          # http://localhost:8080

# Frontend (terminal 2)
cd frontend && npm install
BACKEND_URL=http://localhost:8080 npm run dev # http://localhost:3000
```

## Automated checks

```bash
cd backend  && ./mvnw verify      # unit, repository, MockMvc and integration tests on in-memory H2
cd frontend && npm test           # Vitest: status selector, form errors, error message
```

Expected: all green. `TicketStatusTest` reports 20 pair cases (5 accepted, 15 rejected).

## Scenario 1: Create, list, view (User Story 1; SC-001)

```bash
curl -s -X POST localhost:8080/api/v1/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Printer down","description":"Floor 2 printer offline","priority":"HIGH","status":"CLOSED"}' \
  | jq '{id,status,assignee,allowedNextStatuses}'
```

Expected: `201`; `status` is `OPEN` (the supplied `CLOSED` is ignored), `assignee` is `null`,
`allowedNextStatuses` is `["IN_PROGRESS","CANCELLED"]`. Note the id as `$ID`. Then:

```bash
curl -s "localhost:8080/api/v1/tickets?size=20" | jq '.content[0], .totalElements'   # newest first
curl -s localhost:8080/api/v1/tickets/$ID | jq '{title,description,status,priority,createdAt,updatedAt,comments}'
curl -s -i localhost:8080/api/v1/tickets/999999999                                    # 404 TICKET_NOT_FOUND
```

## Scenario 2: Lifecycle and cancellation (User Story 2; SC-004)

```bash
for s in IN_PROGRESS RESOLVED CLOSED; do
  curl -s -X PATCH localhost:8080/api/v1/tickets/$ID/status -H 'Content-Type: application/json' \
    -d "{\"status\":\"$s\"}" | jq -c '[.status,.allowedNextStatuses,.updatedAt]'
done
```

Expected: each call `200`; statuses go `IN_PROGRESS`, `RESOLVED`, `CLOSED`; after `CLOSED`
`allowedNextStatuses` is `[]`; `updatedAt` changes each step. Create two more tickets: cancel one
while `OPEN`; move the other to `IN_PROGRESS` then cancel. Expected: both `CANCELLED`, and
`GET /api/v1/tickets?status=CANCELLED` lists both.

## Scenario 3: Invalid transitions are rejected (User Story 3; SC-005, SC-006)

```bash
# $ID is CLOSED from scenario 2
curl -s -i -X PATCH localhost:8080/api/v1/tickets/$ID/status -H 'Content-Type: application/json' \
  -d '{"status":"OPEN"}'
```

Expected: `409`, `code = INVALID_STATUS_TRANSITION`, `currentStatus = CLOSED`,
`requestedStatus = OPEN`, `allowedNextStatuses = []`, plain-language `message`. A `GET` afterwards
shows the ticket unchanged, including `updatedAt`.

Also verify each returns `409`: new ticket `OPEN → RESOLVED` and `OPEN → CLOSED`;
`IN_PROGRESS → OPEN`; `RESOLVED → OPEN`; `RESOLVED → CANCELLED`; `CANCELLED → OPEN`; and the same
change sent through `PUT /tickets/{id}` with a `status` field (the rules must not be bypassable).

Unknown value: `PATCH` with `{"status":"DONE"}` returns `400 VALIDATION_ERROR` with a `fieldErrors`
entry for `status` listing the five valid values (not an invalid-transition message).

Same status is not a transition: `PATCH` an `OPEN` ticket with `{"status":"OPEN"}` returns `200`.

## Scenario 4: Update with concurrent-change protection (User Story 4)

```bash
curl -s localhost:8080/api/v1/tickets/$ID | jq .version        # note as $V
curl -s -X PUT localhost:8080/api/v1/tickets/$ID -H 'Content-Type: application/json' \
  -d "{\"title\":\"New title\",\"description\":\"New text\",\"priority\":\"LOW\",\"assignee\":\"Asha\",\"version\":$V}" | jq -c '[.title,.assignee,.version]'
```

Expected: `200`, new values, `version` increased, `updatedAt` changed. Repeat the **same** request
(same old `$V`): expected `409 CONCURRENT_MODIFICATION`, nothing changed. Send `"title":"   "`:
`400` with a `fieldErrors` entry for `title`; nothing changed.

## Scenario 5: Comments (User Story 5)

```bash
curl -s -X POST localhost:8080/api/v1/tickets/$ID/comments -H 'Content-Type: application/json' \
  -d '{"author":"Asha","text":"Replaced the toner."}' | jq -c '[.author,.text,.createdAt]'   # 201
curl -s -X POST localhost:8080/api/v1/tickets/$ID/comments -H 'Content-Type: application/json' \
  -d '{"author":"Asha","text":"   "}'                                                          # 400
curl -s localhost:8080/api/v1/tickets/$ID | jq '.comments | map(.text)'                       # oldest first
```

Expected: comment accepted even on a `CLOSED` or `CANCELLED` ticket; blank comment rejected.

## Scenario 6: Search and filter (User Story 6)

```bash
curl -s "localhost:8080/api/v1/tickets?q=PRINTER" | jq '.totalElements'          # case-insensitive match
curl -s "localhost:8080/api/v1/tickets?status=OPEN" | jq '[.content[].status] | unique'   # ["OPEN"]
curl -s "localhost:8080/api/v1/tickets?status=OPEN&q=printer" | jq '.totalElements'      # both applied
curl -s "localhost:8080/api/v1/tickets?q=%25" | jq '.totalElements'                      # "%" is plain text
```

Expected: matches only; combined filters narrow results; a literal `%` matches only tickets that
contain `%`, and never breaks the query.

## Scenario 7: Simultaneous status changes

Send two `PATCH` requests at the same moment to the same `OPEN` ticket, one `IN_PROGRESS` and one
`CANCELLED` (for example with `&` in a shell). Expected: exactly one wins; the other either succeeds
legitimately against the new status or returns `409` naming the new current status. The ticket never
ends in an impossible state. The automated concurrency test repeats this many times.

## Scenario 8: UI (User Stories 1 to 7; SC-009)

Open `http://localhost:3000/tickets`. Walk through: create a ticket, open it, edit it, add a
comment, search, filter, and change status.

| Ticket status | Status choices shown          |
|---------------|-------------------------------|
| OPEN          | In Progress, Cancelled        |
| IN_PROGRESS   | Resolved, Cancelled           |
| RESOLVED      | Closed                        |
| CLOSED        | none (shown as final)         |
| CANCELLED     | none (shown as final)         |

Error display (Story 7): submit a blank title and an over-length title (message next to the field,
entered text kept); in two tabs change the same ticket's status then submit the now-invalid change
in the first (plain-language server message, screen refreshes to the real status); edit the same
ticket in two tabs and save both (the "changed by someone else" message); open an unknown ticket id
(not-found page with a link to the list); stop the backend and act (friendly message with retry, no
raw technical text). Resize to 320 px wide: the list becomes stacked cards with no horizontal
scrolling; the status filter includes Cancelled.

## Scenario 9: Persistence (SC-008)

Stop and restart the backend. Expected: tickets, comments and statuses are all still present (H2
file in `backend/data/`).

## Performance smoke check

Start the backend with 10,000 seeded tickets. The seeder (`PerfSeedRunner`) lives in test sources,
runs only with the `perf` profile and uses a throw-away in-memory database, so `backend/data/` is
never touched (the `perf` run needs no `TICKETS_DB_PASSWORD`):

```bash
cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=perf
```

Then time the endpoints:

```bash
for url in "/api/v1/tickets?size=20" "/api/v1/tickets?status=OPEN&q=printer" "/api/v1/tickets/$ID"; do
  curl -s -o /dev/null -w "$url %{time_total}s\n" "localhost:8080$url"
done
```

Expected: warm responses under 100 ms. In the browser, Lighthouse on `/tickets` should report first
contentful paint under 1.5 s and under 200 KB gzipped JavaScript on the list page (framework baseline of about 175 KB).
