# Task Breakdown

Work was sequenced bottom-up so each task produced something independently
testable. This reflects the greenfield build (v1) plus the follow-up
enhancement pass that brought it in line with the full spec (v2).

## v1 — Greenfield build

| # | Task | AI assist | Engineer review |
|---|---|---|---|
| 1 | Requirements & scope | — | Wrote `REQUIREMENTS.md`, decided what's out of scope |
| 2 | Project scaffold (`pom.xml`, packages, config) | Generated dependency list | Checked scopes (`test`/`runtime`), versions |
| 3 | Domain model + migration | Generated entity + SQL | Reviewed indexes, nullability |
| 4 | Repository layer | Generated | Checked query correctness |
| 5 | Short code generation | Generated Base62 + sequential-ID first draft | **Rejected sequential IDs** — see `AI_USAGE_LOG.md` |
| 6 | Service layer (cache-aside) | Generated first cache-on-write-only draft | **Fixed** — missing DB fallback on cache miss |
| 7 | Service unit tests | Generated happy-path only | **Added** cache-miss, expiry, conflict cases |
| 8 | Controllers + validation | Generated | Reviewed naming, status codes |
| 9 | Controller tests | Generated | Reviewed |
| 10 | OpenAPI wiring | Generated | — |
| 11 | Docker packaging | Generated | Verified multi-stage build |
| 12 | Documentation | Drafted | Reviewed |

## v2 — Bringing it to the full spec (brownfield enhancement on top of v1)

This is the "Scenario 2 — Brownfield" pattern from the spec, applied to the
project's own v1 → v2 transition:

**Current code → dependency analysis → impact analysis → suggest changes →
generate implementation → generate tests → engineer review**

| # | Task | Impact analysis | Engineer decision |
|---|---|---|---|
| 13 | Rename API paths (`/api/v1/urls` → `/api/v1/shorten`), change request field `originalUrl` → `url` | Touches controllers, DTOs, all existing tests | Accepted as a breaking v1→v2 change since this is pre-release |
| 14 | Duplicate URL detection | New `originalUrlHash` column, new migration, service logic | Custom-alias requests explicitly excluded from dedup — see `TRADEOFFS.md` |
| 15 | `PUT` update endpoint | New DTO, new service method, new controller method | Partial update semantics (omit a field to leave it unchanged) |
| 16 | Analytics (`clicks`/`uniqueVisitors`/`countries`) | New `ClickEvent` entity + table, new repository aggregation queries, new async service | Chose async recording over synchronous — see `TRADEOFFS.md`; chose a header-based geo stub over a real GeoIP DB — see `LIMITATIONS.md` |
| 17 | Rate limiting | New interceptor + Redis keys | Fixed-window over token-bucket — see `TRADEOFFS.md`; excluded actuator/swagger paths after first draft caught them |
| 18 | Retry logic | Wrapped Redis calls | Pulled into a separate `RedisCacheClient` bean — `@Retryable` doesn't work through self-invocation, so this couldn't stay as private methods on the service |
| 19 | Scheduled cleanup job | New `@Scheduled` component | Confirmed idempotent / non-critical-path (expired links are already refused at resolve-time regardless of cleanup timing) |
| 20 | Logging, metrics, health | Cross-cutting | Added SLF4J logging at the actual decision points (created/updated/deleted/duplicate-detected), not blanket entry/exit logging |
| 21 | Regression testing | All v1 tests | **Rewrote every existing test** for the new paths/DTOs rather than leaving stale assertions passing against dead code |
| 22 | Documentation | — | Added `ARCHITECTURE.md`, `TRADEOFFS.md`, `LIMITATIONS.md`; updated the rest |

## If this continued further

Ordered by what would move a real deployment forward:

1. Auth / ownership model (currently anyone can delete anyone's link).
2. A real GeoIP lookup replacing `StubGeoLookupService`.
3. Testcontainers-backed integration tests against real Postgres/Redis.
4. Load testing the redirect path and the rate limiter under concurrency.
5. Structured/correlation-ID logging for multi-instance tracing.
