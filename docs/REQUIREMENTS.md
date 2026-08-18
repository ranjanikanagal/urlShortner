# Requirements

Source: a detailed functional/non-functional spec supplied for this
exercise (API shapes, reliability features, architecture sketch, testing
strategy, and documentation expectations). This doc records how that spec
was read and where a judgment call was made.

## Functional requirements

1. `POST /api/v1/shorten` — create a short URL from `{"url": "..."}`,
   optionally with a custom alias or an explicit expiry date. Response
   includes `shortUrl` and `expiresAt` per spec, plus `shortCode`,
   `originalUrl`, `createdAt` (see `docs/AI_USAGE_LOG.md` for why those were
   added rather than left out).
2. `GET /{shortCode}` — 302 redirect to the original URL.
3. `GET /api/v1/analytics/{code}` — clicks, unique visitors, and a
   per-country breakdown.
4. `PUT /api/v1/shorten/{code}` — update the destination URL and/or expiry
   of an existing short code.
5. `DELETE /api/v1/shorten/{code}` — delete a mapping.

## Reliability features (from the spec)

| Feature | Status | Notes |
|---|---|---|
| Duplicate URL detection | Implemented | Hash-based lookup; only short-circuits generated codes, not custom aliases — see `TRADEOFFS.md` |
| Configurable expiry | Implemented | Explicit `expiresAt` on create/update, or `app.url.default-expiry-days` default |
| Redis cache | Implemented | Cache-aside on the redirect path |
| Rate limiting | Implemented | Redis-backed fixed window, per IP |
| Retry logic | Implemented | `RedisCacheClient` retries transient Redis failures with backoff, degrades gracefully |
| Health endpoint | Implemented | Spring Actuator `/actuator/health` |
| Logging | Implemented | SLF4J across the service/controller layers |
| Metrics | Implemented | Actuator + Micrometer, `/actuator/prometheus` |
| Exception handling | Implemented | Centralized `@RestControllerAdvice` |

## Non-functional requirements

1. **Redirect latency matters most** — the one hot, synchronous path a real
   user waits on. Drives the cache-aside design and the decision to make
   analytics recording asynchronous rather than inline.
2. **Short codes must not collide** — enforced at the DB level, not just in
   application logic.
3. **Schema changes are explicit and auditable** — Flyway migrations, not
   auto-DDL.
4. **The API is self-documenting** — OpenAPI/Swagger.
5. **Input is validated** on every write path before it reaches the
   database.

## Assumptions made

- No auth/multi-tenancy was specified, so this is single-tenant: anyone can
  create, update, or delete any short code. 
- "Countries" in analytics means a coarse country label per click, not a
  full geo/device/browser breakdown — the spec's own hypothetical
  "ambiguous requirement" scenario (Scenario 3) discusses browser/device
  breakdowns as a *separate example* of handling ambiguity, not as part of
  the actual functional requirements, so it wasn't built here.

## Out of scope

- **Kafka** — listed as *optional* in the original suggested stack, and
  nothing in the functional requirements needs an event stream (no
  downstream consumer, no audit-trail requirement). 
- Real GeoIP resolution, full auth/authorization, per-account rate limits,
  custom domains — deliberately left out to keep scope
  matched to the exercise's time box.
