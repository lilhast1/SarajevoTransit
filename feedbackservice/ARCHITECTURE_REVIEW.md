# Feedback Service Review and Delivery Notes

## 1) Current Stage Assessment

The `feedbackservice` is currently at a solid MVP stage and already implements core local-domain web services without dependency on other microservices.

Implemented bounded context:

- Problem reporting (`ProblemReport`)
- Line reviews and moderation (`LineReview`)

Architecture style:

- Layered Spring Boot architecture (`controller -> service -> repository -> model`)
- PostgreSQL persistence through Spring Data JPA
- Input/output DTO layer for API isolation
- Centralized exception handling with `@RestControllerAdvice`
- OpenAPI UI dependency already included

## 2) Consistency Check Across Related Microservices

Compared with `userservice` and project-level patterns:

- Same Spring Boot baseline (`4.0.5`) and Java version (`25`)
- Similar package organization and layered coding style
- Similar exception-handling strategy

Noted consistency gap (kept unchanged intentionally to avoid breaking clients):

- `feedbackservice` uses `/api/v1/...` paths while `userservice` uses `/api/...`.

## 3) Implemented Changes for This Task

All changes were implemented to keep the service independent from other microservices.

### 3.1 DTO Mapping Improvement

Added mapper-based DTO conversion using `ModelMapper`:

- Added dependency: `org.modelmapper:modelmapper:3.2.2`
- Added `ModelMapper` bean configuration
- Added dedicated mapper classes:
  - `ProblemReportMapper`
  - `LineReviewMapper`

Service layer now uses mapper classes instead of repeated manual field-by-field conversion, reducing duplication and improving maintainability.

### 3.2 New Local Web Service Endpoints (No Cross-Service Calls)

Added endpoints that work only with local feedback database data:

- `GET /api/v1/reports/line/{lineId}`
- `GET /api/v1/reviews/{id}`
- `GET /api/v1/reviews/reviewer/{reviewerUserId}`

### 3.3 Data Integrity and Service Robustness

Improved validation and transaction boundaries:

- Added `@Positive` validation for:
  - `CreateProblemReportRequest.reporterUserId`
  - `CreateLineReviewRequest.reviewerUserId`
- Added explicit transactional semantics in services:
  - `@Transactional` for writes
  - `@Transactional(readOnly = true)` for reads

### 3.4 Test Stability Without External PostgreSQL

To keep local development and CI stable without requiring a running PostgreSQL instance:

- Added test dependency: `com.h2database:h2`
- Added `src/test/resources/application-test.properties` with in-memory H2 setup (PostgreSQL mode)
- Activated test profile in `FeedbackserviceApplicationTests` via `@ActiveProfiles("test")`

## 4) Why This Meets the Assignment

Assignment focus: create web services that do not require communication with other microservices.

This service now provides a broader set of self-contained REST endpoints over local domain data only, with clear DTO boundaries and stronger service-layer design.

## 5) Forward-Looking Recommendations

1. Standardize API versioning strategy across all microservices (`/api/v1` vs `/api`).
2. Add focused controller/service tests for new endpoints (including validation and error flows).
3. Consider introducing pagination for list endpoints that can grow (`reports`, `reviews`).
4. Replace `spring.jpa.hibernate.ddl-auto=update` in production with migration tooling (Flyway/Liquibase).
5. Move seed data (`DataInitializer`) behind a dedicated profile to keep production startup deterministic.

## 6) Documentation Deliverables for Assignment

The following artifacts were added for service documentation and request-response evidence:

- `API_DOCUMENTATION.md` for endpoint-level documentation.
- `API_TEST_REPORT.md` for successful and unsuccessful request-response cases.
- `docs/api-test-evidence/*.json` with captured test outputs.
- `docs/postman/feedbackservice.postman_collection.json` for Postman execution.
- `docs/postman-screenshots/README.md` with required screenshot file naming.
