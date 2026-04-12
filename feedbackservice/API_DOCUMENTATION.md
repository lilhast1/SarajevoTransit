# Feedback Service API Documentation

This documentation uses the requested API-call template format for each endpoint.

Base URL: http://localhost:8080
Content-Type: application/json

Common error envelope:

```json
{
  "timestamp": "2026-04-11T18:33:56.7114836Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "category": "must not be null"
  }
}
```

## 1) Create Problem Report

Title

- Create Problem Report

URL

- /api/v1/reports

Method

- POST

URL Params

- Required: none
- Optional: none

Data Params

```json
{
  "reporterUserId": "[integer, required, > 0]",
  "lineId": "[integer, optional, > 0]",
  "vehicleId": "[integer, optional, > 0]",
  "vehicleRegistrationNumber": "[string, optional, max 60]",
  "vehicleInternalId": "[string, optional, max 60]",
  "vehicleType": "[string, optional, BUS|TRAM|TROLLEY|MINIBUS, max 30]",
  "stationId": "[integer, optional, > 0]",
  "category": "[enum, required, BREAKDOWN|CROWDING|HYGIENE|AGGRESSIVE_BEHAVIOR|DELAY|OTHER]",
  "description": "[string, required, not blank, max 1000]",
  "photoUrls": ["[string, optional, each max 500]"]
}
```

Example:

```json
{
  "reporterUserId": 5101,
  "lineId": 6,
  "stationId": 18,
  "category": "DELAY",
  "description": "Delay around 8 minutes at evening peak.",
  "photoUrls": ["https://example.com/evidence/report-ok.png"]
}
```

Success Response

```text
Code: 201 Created
Content: {
  "id": 101,
  "reporterUserId": 5101,
  "lineId": 6,
  "category": "DELAY",
  "status": "RECEIVED"
}
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Validation failed", "fieldErrors": { ... } }
```

OR

```text
Code: 400 Bad Request
Content: { "message": "At least one of vehicleId/vehicleRegistrationNumber/vehicleInternalId or stationId must be provided." }
```

Sample Call

```bash
curl -X POST "http://localhost:8080/api/v1/reports" \
  -H "Content-Type: application/json" \
  -d '{
    "reporterUserId": 5101,
    "lineId": 6,
    "stationId": 18,
    "category": "DELAY",
    "description": "Delay around 8 minutes at evening peak.",
    "photoUrls": ["https://example.com/evidence/report-ok.png"]
  }'
```

Notes

- 2026-04-12 (Copilot): At least one vehicle reference field or stationId must be provided.

## 2) Show All Problem Reports

Title

- Show All Problem Reports

URL

- /api/v1/reports

Method

- GET

URL Params

- Required: none
- Optional:
  - status=[enum: RECEIVED|IN_PROGRESS|RESOLVED]
  - reporterUserId=[integer]

Data Params

- none

Success Response

```text
Code: 200 OK
Content: [ { "id": 101, "reporterUserId": 5101, "status": "RECEIVED" } ]
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Malformed or invalid query parameter." }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reports?status=RECEIVED&reporterUserId=5101"
```

Notes

- 2026-04-12 (Copilot): Results are sorted by createdAt descending.

## 3) Show One Problem Report

Title

- Show One Problem Report

URL

- /api/v1/reports/:id

Method

- GET

URL Params

- Required:
  - id=[integer]
  - example: id=101
- Optional: none

Data Params

- none

Success Response

```text
Code: 200 OK
Content: { "id": 101, "reporterUserId": 5101, "status": "RECEIVED" }
```

Error Response

```text
Code: 404 Not Found
Content: { "message": "Problem report not found: id=101" }
```

OR

```text
Code: 400 Bad Request
Content: { "message": "Validation failed" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reports/101"
```

Notes

- 2026-04-12 (Copilot): id must be a positive existing report id.

## 4) Show Problem Reports by Line

Title

- Show Problem Reports by Line

URL

- /api/v1/reports/line/:lineId

Method

- GET

URL Params

- Required:
  - lineId=[integer, > 0]
  - example: lineId=33
- Optional: none

Data Params

- none

Success Response

```text
Code: 200 OK
Content: [ { "id": 120, "lineId": 33, "category": "CROWDING" } ]
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Validation failed" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reports/line/33"
```

Notes

- 2026-04-12 (Copilot): Useful for line-focused operations and moderation views.

## 5) Update Problem Report Status

Title

- Update Problem Report Status

URL

- /api/v1/reports/:id/status

Method

- PATCH

URL Params

- Required:
  - id=[integer]
  - example: id=101
- Optional: none

Data Params

```json
{
  "status": "[enum, required, RECEIVED|IN_PROGRESS|RESOLVED]"
}
```

Example:

```json
{
  "status": "IN_PROGRESS"
}
```

Success Response

```text
Code: 200 OK
Content: { "id": 101, "status": "IN_PROGRESS" }
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Malformed JSON request" }
```

OR

```text
Code: 404 Not Found
Content: { "message": "Problem report not found: id=101" }
```

Sample Call

```bash
curl -X PATCH "http://localhost:8080/api/v1/reports/101/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

Notes

- 2026-04-12 (Copilot): This endpoint updates only lifecycle status.

## 6) Create Line Review

Title

- Create Line Review

URL

- /api/v1/reviews

Method

- POST

URL Params

- Required: none
- Optional: none

Data Params

```json
{
  "reviewerUserId": "[integer, required, > 0]",
  "lineId": "[integer, required, > 0]",
  "rating": "[integer, required, 1..5]",
  "reviewText": "[string, optional, max 1500]",
  "rideDate": "[date, required, yyyy-MM-dd]"
}
```

Example:

```json
{
  "reviewerUserId": 5201,
  "lineId": 6,
  "rating": 4,
  "reviewText": "Ride was stable with medium crowd.",
  "rideDate": "2026-04-10"
}
```

Success Response

```text
Code: 201 Created
Content: { "id": 201, "reviewerUserId": 5201, "lineId": 6, "rating": 4, "moderationStatus": "VISIBLE" }
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Validation failed", "fieldErrors": { ... } }
```

OR

```text
Code: 400 Bad Request
Content: { "message": "rideDate cannot be in the future." }
```

Sample Call

```bash
curl -X POST "http://localhost:8080/api/v1/reviews" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewerUserId": 5201,
    "lineId": 6,
    "rating": 4,
    "reviewText": "Ride was stable with medium crowd.",
    "rideDate": "2026-04-10"
  }'
```

Notes

- 2026-04-12 (Copilot): rideDate cannot be future and must be within last 30 days.

## 7) Show Line Reviews by Line

Title

- Show Line Reviews by Line

URL

- /api/v1/reviews

Method

- GET

URL Params

- Required:
  - lineId=[integer, > 0]
  - example: lineId=6
- Optional:
  - includeHidden=[boolean, default false]

Data Params

- none

Success Response

```text
Code: 200 OK
Content: [ { "id": 201, "lineId": 6, "rating": 4, "moderationStatus": "VISIBLE" } ]
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Validation failed" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reviews?lineId=6&includeHidden=false"
```

Notes

- 2026-04-12 (Copilot): includeHidden=false returns only VISIBLE reviews.

## 8) Show One Line Review

Title

- Show One Line Review

URL

- /api/v1/reviews/:id

Method

- GET

URL Params

- Required:
  - id=[integer, > 0]
  - example: id=201
- Optional: none

Data Params

- none

Success Response

```text
Code: 200 OK
Content: { "id": 201, "reviewerUserId": 5201, "lineId": 6, "rating": 4, "moderationStatus": "VISIBLE" }
```

Error Response

```text
Code: 404 Not Found
Content: { "message": "Line review not found: id=201" }
```

OR

```text
Code: 400 Bad Request
Content: { "message": "Validation failed" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reviews/201"
```

Notes

- 2026-04-12 (Copilot): id must be positive and existing.

## 9) Show Line Reviews by Reviewer

Title

- Show Line Reviews by Reviewer

URL

- /api/v1/reviews/reviewer/:reviewerUserId

Method

- GET

URL Params

- Required:
  - reviewerUserId=[integer, > 0]
  - example: reviewerUserId=5201
- Optional: none

Data Params

- none

Success Response

```text
Code: 200 OK
Content: [ { "id": 201, "reviewerUserId": 5201, "lineId": 6 } ]
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Validation failed" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reviews/reviewer/5201"
```

Notes

- 2026-04-12 (Copilot): Sorted by createdAt descending.

## 10) Update Review Moderation Status

Title

- Update Review Moderation Status

URL

- /api/v1/reviews/:id/moderation-status

Method

- PATCH

URL Params

- Required:
  - id=[integer]
  - example: id=201
- Optional: none

Data Params

```json
{
  "moderationStatus": "[enum, required, VISIBLE|HIDDEN]"
}
```

Example:

```json
{
  "moderationStatus": "HIDDEN"
}
```

Success Response

```text
Code: 200 OK
Content: { "id": 201, "moderationStatus": "HIDDEN" }
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Malformed JSON request" }
```

OR

```text
Code: 404 Not Found
Content: { "message": "Line review not found: id=201" }
```

Sample Call

```bash
curl -X PATCH "http://localhost:8080/api/v1/reviews/201/moderation-status" \
  -H "Content-Type: application/json" \
  -d '{"moderationStatus":"HIDDEN"}'
```

Notes

- 2026-04-12 (Copilot): This endpoint changes only moderation visibility.

## 11) Show Line Rating Summary for All Lines

Title

- Show Line Rating Summary for All Lines

URL

- /api/v1/reviews/summary

Method

- GET

URL Params

- Required: none
- Optional: none

Data Params

- none

Success Response

```text
Code: 200 OK
Content: [ { "lineId": 6, "averageRating": 4.5, "totalReviews": 10 } ]
```

Error Response

```text
Code: 500 Internal Server Error
Content: { "message": "Unexpected error" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reviews/summary"
```

Notes

- 2026-04-12 (Copilot): Summaries are calculated from VISIBLE reviews.

## 12) Show Line Rating Summary for One Line

Title

- Show Line Rating Summary for One Line

URL

- /api/v1/reviews/summary/:lineId

Method

- GET

URL Params

- Required:
  - lineId=[integer, > 0]
  - example: lineId=6
- Optional: none

Data Params

- none

Success Response

```text
Code: 200 OK
Content: { "lineId": 6, "averageRating": 4.5, "totalReviews": 10 }
```

Error Response

```text
Code: 400 Bad Request
Content: { "message": "Validation failed" }
```

Sample Call

```bash
curl "http://localhost:8080/api/v1/reviews/summary/6"
```

Notes

- 2026-04-12 (Copilot): If no visible reviews exist, returns averageRating=0.0 and totalReviews=0.

## Assignment artifacts

- Evidence JSON files:
  - docs/api-test-evidence/report-success.json
  - docs/api-test-evidence/report-failure.json
  - docs/api-test-evidence/review-success.json
  - docs/api-test-evidence/review-failure.json
- Postman collection:
  - docs/postman/feedbackservice.postman_collection.json
- Screenshot markdown:
  - docs/postman-screenshots/README.md
