# User Service Architecture Review and Delivery Notes

## 1) Current Stage Assessment

`userservice` is at a strong MVP stage and now covers local-domain REST functionality expected for the assignment.

Implemented bounded context:

- User profile and account data.
- User accessibility/notification preferences.
- User travel and purchase history.
- Loyalty points wallet and transactions.

Architecture style:

- Layered Spring Boot architecture (`controller -> service -> repository -> model`).
- PostgreSQL persistence with Spring Data JPA.
- DTO-driven API contracts.
- Centralized API exception handling.
- OpenAPI UI dependency available.

## 2) Consistency Check with Related Microservices

Compared with sibling services (`feedbackservice`, `notificationservice`, `vehicleservice`):

- Same Spring Boot baseline (`4.0.5`).
- Similar package organization and layered structure.
- Similar use of JPA + validation + global exception handling.

Observed consistency gap and mitigation:

- `feedbackservice` primarily exposes `/api/v1/...` paths, while `userservice` previously exposed `/api/...` only.
- `userservice` now supports both prefixes (`/api/users` and `/api/v1/users`) to improve consistency without breaking existing clients.

## 3) Changes Implemented in This Delivery

### 3.1 New Local-Only REST Endpoints

Added endpoints that rely only on `userservice` local data:

- `GET /api/v1/users/{userId}/preferences`
- `GET /api/v1/users/{userId}/travel-history`
- `GET /api/v1/users/{userId}/ticket-purchases`

### 3.2 API Versioning Compatibility

Updated controller mappings to support both:

- `/api/users...`
- `/api/v1/users...`

This provides cross-service path consistency while preserving backward compatibility.

### 3.3 Validation and Error Handling Hardening

Improved method parameter and request parsing behavior:

- Added positive id validation for path variables.
- Added `limit` validation for suggestions endpoint (`1..10`).
- Added dedicated handlers for:
  - `ConstraintViolationException`
  - `MethodArgumentTypeMismatchException`
  - `HttpMessageNotReadableException`

Result: malformed JSON and invalid path/query values return `400 Bad Request` with consistent error payloads instead of generic `500`.

### 3.4 Integration Testing Upgrade

Added focused controller integration tests with `MockMvc` and test profile:

- User API integration tests:
  - create user
  - retrieve travel history
  - invalid suggestions limit
  - invalid user id path variable
- Loyalty API integration tests:
  - earn points + verify balance
  - reject redeem without balance
  - malformed JSON handling
  - invalid user id path variable

### 3.5 Documentation and Submission Evidence

Added assignment artifacts for API documentation and proof of execution:

- `API_TEST_REPORT.md` with success and failure scenarios.
- `docs/api-test-evidence/*.json` request-response captures.
- `docs/postman/userservice.postman_collection.json` for reproducible execution.
- `docs/postman-screenshots/README.md` describing required screenshot filenames.

## 4) Why This Meets the Assignment

Assignment requirement: create web services that do not require communication with other microservices and use DTOs where appropriate.

Delivered solution:

- All implemented endpoints are local-only and backed by local repositories.
- API contracts are DTO-based for both request and response.
- REST patterns and validation align with Spring guide recommendations.

## 5) Insights and Forward Recommendations

1. Standardize Java version across all services (`21`, `25`, and `26` are currently mixed).
2. Standardize API prefix convention (`/api/v1`) across all microservices.
3. Add pagination for list endpoints to improve scalability.
4. Introduce DB migration tooling (Flyway/Liquibase) and avoid relying on `ddl-auto=update` in production.
5. Consider extracting shared error-contract conventions into a common internal guideline for all service teams.
