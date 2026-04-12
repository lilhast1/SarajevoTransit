# Feedback Service API Documentation

## 1. Service Overview

Feedback Service manages two local domain areas:

1. Problem reports submitted by passengers.
2. Line reviews and moderation.

The service is self contained and does not require synchronous calls to other microservices.

## 2. Base URL and Format

- Base URL: http://localhost:8080
- API prefix: /api/v1
- Content type: application/json
- Time zone in responses: UTC

## 3. Enums

### ProblemCategory

- BREAKDOWN
- CROWDING
- HYGIENE
- AGGRESSIVE_BEHAVIOR
- DELAY
- OTHER

### ReportStatus

- RECEIVED
- IN_PROGRESS
- RESOLVED

### ModerationStatus

- VISIBLE
- HIDDEN

## 4. Standard Error Model

Validation and business errors are returned as JSON:

```json
{
  "timestamp": "2026-04-11T18:33:56.7114836Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "category": "must not be null",
    "description": "must not be blank"
  }
}
```

## 5. Problem Report API

### 5.1 Create report

- Method: POST
- Path: /api/v1/reports
- Success: 201 Created

Minimal valid request body:

```json
{
  "reporterUserId": 4101,
  "lineId": 5,
  "stationId": 15,
  "category": "DELAY",
  "description": "Delay around 10 minutes at peak time.",
  "photoUrls": ["https://example.com/evidence/delay-2.jpg"]
}
```

Validation notes:

- reporterUserId is required and must be positive.
- category is required.
- description is required and must not be blank.
- at least one vehicle reference or stationId must be provided.

### 5.2 List reports

- Method: GET
- Path: /api/v1/reports
- Query params:
  - status (optional)
  - reporterUserId (optional)
- Success: 200 OK

### 5.3 Get report by id

- Method: GET
- Path: /api/v1/reports/{id}
- Success: 200 OK
- Failure: 404 Not Found

### 5.4 Get reports by line id

- Method: GET
- Path: /api/v1/reports/line/{lineId}
- Success: 200 OK
- Validation failure: 400 Bad Request for non positive lineId

### 5.5 Update report status

- Method: PATCH
- Path: /api/v1/reports/{id}/status
- Success: 200 OK

Request body:

```json
{
  "status": "IN_PROGRESS"
}
```

## 6. Line Review API

### 6.1 Create review

- Method: POST
- Path: /api/v1/reviews
- Success: 201 Created

Minimal valid request body:

```json
{
  "reviewerUserId": 4201,
  "lineId": 5,
  "rating": 4,
  "reviewText": "Ride was okay with medium crowd.",
  "rideDate": "2026-04-10"
}
```

Validation notes:

- reviewerUserId is required and must be positive.
- lineId is required and must be positive.
- rating must be in range 1 to 5.
- rideDate is required and must not be in the future.
- rideDate must be within last 30 days.

### 6.2 List reviews by line

- Method: GET
- Path: /api/v1/reviews
- Query params:
  - lineId (required)
  - includeHidden (optional, default false)
- Success: 200 OK

### 6.3 Get review by id

- Method: GET
- Path: /api/v1/reviews/{id}
- Success: 200 OK
- Failure: 404 Not Found

### 6.4 Get reviews by reviewer

- Method: GET
- Path: /api/v1/reviews/reviewer/{reviewerUserId}
- Success: 200 OK

### 6.5 Update moderation status

- Method: PATCH
- Path: /api/v1/reviews/{id}/moderation-status
- Success: 200 OK

Request body:

```json
{
  "moderationStatus": "HIDDEN"
}
```

### 6.6 Rating summary for all lines

- Method: GET
- Path: /api/v1/reviews/summary
- Success: 200 OK

### 6.7 Rating summary for one line

- Method: GET
- Path: /api/v1/reviews/summary/{lineId}
- Success: 200 OK

## 7. Documentation and Testing Artifacts

- API test evidence (real request response captures):
  - docs/api-test-evidence/report-success.json
  - docs/api-test-evidence/report-failure.json
  - docs/api-test-evidence/review-success.json
  - docs/api-test-evidence/review-failure.json
- Postman collection:
  - docs/postman/feedbackservice.postman_collection.json
- Screenshot folder for assignment submission:
  - docs/postman-screenshots/
