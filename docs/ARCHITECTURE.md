# Architecture

```
                Client
                  |
            REST Controller
        (UrlController / RedirectController / AnalyticsController)
                  |
          URL Service Layer
        /         |          \
 Validation   Cache (Redis)   Analytics Service (async)
                  |                    |
            PostgreSQL  <--------------+
                  |
             Background Jobs
        (Expired URL Cleanup, @Scheduled)
```

## Request paths

**Create (`POST /api/v1/shorten`)** — validate → duplicate-hash lookup →
generate/claim short code → persist to Postgres → populate Redis →
respond. Postgres is written before the cache, so a crash between the two
steps loses at worst a cache warm-up, never data.

**Redirect (`GET /{shortCode}`)** — the hot path. Redis lookup first; on a
hit, respond immediately with the 302 and hand off click recording to an
async executor. On a miss, read Postgres, populate Redis, then respond. The
redirect response is never blocked on the analytics write.

**Analytics (`GET /api/v1/analytics/{code}`)** — reads the denormalized
`clickCount` on `url_mapping` for the total, and aggregates the
`click_event` table (written by the async path above) for unique visitors
and per-country breakdown.

**Update (`PUT /api/v1/shorten/{code}`)** / **Delete (`DELETE
/api/v1/shorten/{code}`)** — straightforward read-modify-write against
Postgres, with the corresponding cache entry updated or evicted in the same
call.

## Cross-cutting concerns

- **Rate limiting** sits in front of the controller layer as a
  `HandlerInterceptor`, backed by Redis `INCR`/`EXPIRE` so limits are
  enforced correctly across multiple app instances, not just per-process.
- **Retry** wraps every Redis call (`RedisCacheClient`) with bounded
  exponential backoff and a recover step that degrades to a cache miss
  rather than failing the request — Redis being briefly unavailable should
  never take the API down.
- **Background cleanup** runs on a fixed schedule, independent of the
  request path, purging expired mappings from both Postgres and Redis.

