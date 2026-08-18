# urlShortner
AI Assessment
URL Shortener Service
A production-shaped URL shortener built as an AI-assisted engineering exercise. The point of the exercise is the process around the code as much as the code itself — 
see docs/ for the requirements breakdown, task plan, AI usage log, architecture that produced this build.

Stack
Concern	Choice
Language/runtime	Java 21
Framework	Spring Boot 3.3 (Web, Data JPA, Validation, Actuator, AOP)
Database	PostgreSQL (schema managed by Flyway)
Cache	Redis (cache-aside for redirects, backing store for rate limiting)
Build	Maven
API docs	springdoc-openapi / Swagger UI
Metrics	Micrometer + Prometheus (/actuator/prometheus)
Tests	JUnit 5 + Mockito + Spring @WebMvcTest slices
Packaging	Docker / Docker Compose
Kafka is not implemented — it was optional in the suggested stack and nothing in the requirements needs an event stream. See docs/REQUIREMENTS.md
Running it
Option A — Docker Compose (everything)
docker compose up --build
Starts Postgres, Redis, and the app on http://localhost:8080. Flyway runs migrations automatically on boot.

Option B — Local JVM, containerized dependencies
docker compose up postgres redis
mvn spring-boot:run
Running the tests
mvn test

API
Interactive docs: http://localhost:8080/swagger-ui.html.

Method	Path	Description
POST	/api/v1/shorten	Create a short URL
GET	/{shortCode}	302 redirect to the original URL
PUT	/api/v1/shorten/{shortCode}	Update the destination URL and/or expiry
DELETE	/api/v1/shorten/{shortCode}	Delete a mapping
GET	/api/v1/analytics/{shortCode}	Clicks, unique visitors, country breakdown
Create a short URL

curl -X POST http://localhost:8080/api/v1/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com/very/long/url"}'
{
  "shortCode": "Ax7KfP",
  "shortUrl": "http://localhost:8080/Ax7KfP",
  "originalUrl": "https://www.example.com/very/long/url",
  "createdAt": "2026-08-15T10:00:00Z",
  "expiresAt": "2027-08-15"
}
Optional fields: customAlias (4–16 alphanumeric chars, rejected if taken) and expiresAt (date; defaults to app.url.default-expiry-days from now if omitted). Submitting a URL that's already been shortened (and not customized with an alias) returns the existing short code instead of creating a duplicate.

Get analytics

curl http://localhost:8080/api/v1/analytics/Ax7KfP
{
  "clicks": 1045,
  "uniqueVisitors": 810,
  "countries": {"US": 420, "India": 300}
}
Update

curl -X PUT http://localhost:8080/api/v1/shorten/Ax7KfP \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com/new-destination"}'
Reliability features
Redis cache-aside on the redirect path, with retry + graceful degradation to a cache miss if Redis is briefly unavailable (RedisCacheClient).
Rate limiting — Redis-backed fixed window, per client IP, on the create and redirect endpoints (app.rate-limit.* in application.yml).
Duplicate URL detection via a hashed lookup, scoped to non-custom-alias requests.
Configurable expiry, explicit or defaulted.
Async analytics — click recording happens off the redirect's response path on a dedicated thread pool.
Scheduled cleanup job purges expired mappings from Postgres and Redis.
Health, logging, metrics, centralized exception handling — Actuator /actuator/health, SLF4J throughout the service/controller layers, Micrometer/Prometheus, and a @RestControllerAdvice.

Project documentation
docs/REQUIREMENTS.md — functional/non-functional requirements, assumptions, out-of-scope items
docs/TASK_BREAKDOWN.md — how the work was decomposed and sequenced (greenfield build + brownfield enhancement pass)
docs/ARCHITECTURE.md — component diagram and request-path walkthrough
docs/AI_USAGE_LOG.md — where AI helped, what a human reviewed/changed and why
docs/TESTING.md — test strategy and what's covered vs. not
