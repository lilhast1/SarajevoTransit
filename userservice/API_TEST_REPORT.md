# User Service API Test Report

## 1. Scope

This report documents assignment-required API testing for `userservice` with one successful and one failed request-response per implemented domain flow.

Execution tool: Postman (collection included in project).

## 2. Success and Failure Cases

### 2.1 User create - success

- Test id: `US-S-01`
- Endpoint: `POST /api/v1/users`
- Expected: `201 Created`
- Evidence file: `docs/api-test-evidence/user-create-success.json`
- Screenshot target: `docs/postman-screenshots/user-create-success.png`

### 2.2 User create - failure

- Test id: `US-F-01`
- Endpoint: `POST /api/v1/users`
- Scenario: invalid format + incomplete payload
- Expected: `400 Bad Request`
- Evidence file: `docs/api-test-evidence/user-create-failure.json`
- Screenshot target: `docs/postman-screenshots/user-create-failure.png`

### 2.3 Loyalty earn - success

- Test id: `LS-S-01`
- Endpoint: `POST /api/v1/users/{userId}/loyalty/earn`
- Expected: `200 OK`
- Evidence file: `docs/api-test-evidence/loyalty-earn-success.json`
- Screenshot target: `docs/postman-screenshots/loyalty-earn-success.png`

### 2.4 Loyalty redeem - failure

- Test id: `LS-F-01`
- Endpoint: `POST /api/v1/users/{userId}/loyalty/redeem`
- Scenario: insufficient points for redemption
- Expected: `400 Bad Request`
- Evidence file: `docs/api-test-evidence/loyalty-redeem-failure.json`
- Screenshot target: `docs/postman-screenshots/loyalty-redeem-failure.png`

## 3. Postman Collection

- File: `docs/postman/userservice.postman_collection.json`
- Variables:
  - `baseUrl` default `http://localhost:8080`
  - `userId` set after successful user creation

## 4. How to produce required screenshots

1. Start `userservice` locally.
2. Import `docs/postman/userservice.postman_collection.json` into Postman.
3. Execute one success request and one failure request.
4. Capture Postman screenshot for each executed case.
5. Save screenshots under `docs/postman-screenshots/` with names listed in `docs/postman-screenshots/README.md`.

## 5. Notes

- Test response samples are stored in JSON evidence files for reproducibility.
- Error responses follow centralized API error format from GlobalExceptionHandler.
