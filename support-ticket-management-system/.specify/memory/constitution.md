<!--
Sync Impact Report
- Version change: (unfilled template) → 1.0.0
- Modified principles: none renamed; all five template principle slots filled
  - [PRINCIPLE_1_NAME] → I. Simplicity First (MVP Scope)
  - [PRINCIPLE_2_NAME] → II. Layered Spring Boot Backend
  - [PRINCIPLE_3_NAME] → III. REST API Contract
  - [PRINCIPLE_4_NAME] → IV. Test What Matters
  - [PRINCIPLE_5_NAME] → V. Secure by Default
- Added sections: Technology Stack, Development Workflow
- Removed sections: none
- Follow-up TODOs: none
- Templates reviewed (not modified here): plan-template.md, spec-template.md, tasks-template.md
-->
# Support Ticket Management System Constitution

## Core Principles

### I. Simplicity First (MVP Scope)
The system MUST deliver only the minimum ticket-management capability: create, view, list,
update, assign, change status of, and comment on support tickets. Features beyond this (SLA
timers, email integration, reporting, multi-tenancy) MUST NOT be built without an approved spec.
Rationale: a small, working core is easier to verify and extend than a broad, half-finished one.

### II. Layered Spring Boot Backend
The backend MUST be a single Spring Boot application on Java 21 organized as controller →
service → repository. Controllers MUST NOT contain business rules or access repositories
directly. Entities MUST NOT be exposed through the API; DTOs MUST be used at the boundary.
Rationale: clear layering keeps logic testable and the API stable when the schema changes.

### III. REST API Contract
All backend functionality MUST be exposed through JSON REST endpoints under `/api/v1`.
Endpoints MUST use correct HTTP verbs and status codes, validate input with Bean Validation,
and return errors in one consistent structure (status, message, field errors) via a global
exception handler. The frontend MUST communicate with the backend only through this API.
Rationale: a stable contract lets frontend and backend be developed and tested independently.

### IV. Test What Matters
Every service-layer business rule and every REST endpoint MUST have automated tests. Tests MUST
run on H2 and MUST pass before a change is merged. Bug fixes MUST include a regression test.
Rationale: ticket status and assignment rules are the core value and must not regress silently.

### V. Secure by Default
Secrets and credentials MUST NOT be committed; they MUST come from environment variables or
external configuration. All endpoints except login/health MUST require authentication, and
changes MUST be authorized by role (at minimum: customer, agent, admin). Input MUST be
validated server-side, and queries MUST be parameterized (JPA/JDBC bindings only).
Rationale: tickets contain customer data; insecure defaults are costly to fix later.

## Technology Stack

- **Backend**: Java 21, Spring Boot, Spring Web, Spring Data JPA, Bean Validation, Maven or
  Gradle (one build tool, pinned versions).
- **Database**: H2 (in-memory) for local development and automated tests; PostgreSQL for
  production. Schema changes MUST be managed by versioned migrations (Flyway or Liquibase),
  not by `hibernate.ddl-auto` in production.
- **API**: JSON REST over HTTP, documented with OpenAPI.
- **Frontend**: React with Next.js (or an equivalent React-based framework), using TypeScript.
- New dependencies MUST be justified in the plan and MUST use well-maintained libraries.

## Development Workflow

- Work happens on feature branches; direct commits to `main` are not allowed.
- Every feature follows the Spec Kit flow: specify → plan → tasks → implement.
- Changes MUST build, pass lint/format checks, and pass all tests before review.
- Pull requests MUST be small and focused, and MUST state what changed and why.

## Governance

This constitution supersedes other practices. Amendments MUST be made via the
`/speckit-constitution` command, documented with a Sync Impact Report, and versioned using
semantic versioning: MAJOR for removed or redefined principles, MINOR for added principles or
materially expanded guidance, PATCH for clarifications. Every plan and pull request MUST be
checked against these principles, and any deviation MUST be justified in writing in the plan.

**Version**: 1.0.0 | **Ratified**: 2026-11-22 | **Last Amended**: 2026-11-22
