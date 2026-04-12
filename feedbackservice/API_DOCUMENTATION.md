# Feedback Service API Documentation

This document follows the required API-call template format:

- Title
- URL
- Method
- URL Params
- Data Params
- Success Response
- Error Response
- Sample Call
- Notes

Base API path used in examples: `/api/v1`

## 1) Create One Problem Report

- Title: Create One Problem Report
- URL: `/api/v1/reports`
- Method: `POST`
- URL Params:
  - Required: none
  - Optional: none
- Data Params:

```json
{
  "reporterUserId": "[integer, required, > 0]",
  "lineId": "[integer, optional, > 0]",
  "vehicleId": "[integer, optional, > 0]",
  "vehicleRegistrationNumber": "[string, optional, max 60]",
  "vehicleInternalId": "[string, optional, max 60]",
  "vehicleType": "[string, optional, BUS|TRAM|TROLLEY|MINIBUS]",
  "stationId": "[integer, optional, > 0]",
  "category": "[enum, required, BREAKDOWN|CROWDING|HYGIENE|AGGRESSIVE_BEHAVIOR|DELAY|OTHER]",
  "description": "[string, required, not blank, max 1000]",
  "photoUrls": "[array of strings, optional, each max 500]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `{ "id": 501, "status": "RECEIVED" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "At least one of vehicleId/vehicleRegistrationNumber/vehicleInternalId or stationId must be provided." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/reports" \
  --header "Content-Type: application/json" \
  --data "{\"reporterUserId\":5101,\"lineId\":6,\"stationId\":18,\"category\":\"DELAY\",\"description\":\"Delay around 8 minutes at evening peak.\"}"
```

- Notes:
  - 2026-04-12 (Copilot): New reports are always created with status `RECEIVED`.

## 2) Show Problem Reports

- Title: Show Problem Reports
- URL: `/api/v1/reports?status=:status&reporterUserId=:reporterUserId`
- Method: `GET`
- URL Params:
  - Required: none
  - Optional:
    - `status=[enum: RECEIVED|IN_PROGRESS|RESOLVED]`
      - Example: `status=IN_PROGRESS`
    - `reporterUserId=[integer]`
      - Example: `reporterUserId=5101`
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "id": 501, "status": "RECEIVED" } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reports?status=RECEIVED"
```

- Notes:
  - 2026-04-12 (Copilot): Supports combined filtering by `status` and `reporterUserId`.

## 3) Show One Problem Report

- Title: Show One Problem Report
- URL: `/api/v1/reports/:id`
- Method: `GET`
- URL Params:
  - Required:
    - `id=[integer]`
      - Example: `id=501`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `{ "id": 501, "status": "RECEIVED" }`
- Error Response:
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "Problem report not found: id=501" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reports/501"
```

- Notes:
  - 2026-04-12 (Copilot): Returns one `ProblemReportResponse` object.

## 4) Show Problem Reports By Line

- Title: Show Problem Reports By Line
- URL: `/api/v1/reports/line/:lineId`
- Method: `GET`
- URL Params:
  - Required:
    - `lineId=[integer > 0]`
      - Example: `lineId=6`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "id": 501, "lineId": 6 } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reports/line/6"
```

- Notes:
  - 2026-04-12 (Copilot): `lineId` must be strictly positive.

## 5) Update One Problem Report Status

- Title: Update One Problem Report Status
- URL: `/api/v1/reports/:id/status`
- Method: `PATCH`
- URL Params:
  - Required:
    - `id=[integer]`
      - Example: `id=501`
  - Optional: none
- Data Params:

```json
{
  "status": "[enum, required, RECEIVED|IN_PROGRESS|RESOLVED]"
}
```

- Success Response:
  - Code: `200 OK`
  - Content: `{ "id": 501, "status": "IN_PROGRESS" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "Problem report not found: id=501" }`
- Sample Call:

```bash
curl --request PATCH "http://localhost:8080/api/v1/reports/501/status" \
  --header "Content-Type: application/json" \
  --data "{\"status\":\"IN_PROGRESS\"}"
```

- Notes:
  - 2026-04-12 (Copilot): Uses partial update semantics (`PATCH`).

## 6) Create One Line Review

- Title: Create One Line Review
- URL: `/api/v1/reviews`
- Method: `POST`
- URL Params:
  - Required: none
  - Optional: none
- Data Params:

```json
{
  "reviewerUserId": "[integer, required, > 0]",
  "lineId": "[integer, required, > 0]",
  "rating": "[integer, required, 1..5]",
  "reviewText": "[string, optional, max 1500]",
  "rideDate": "[date, required, not in future, within last 30 days]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `{ "id": 801, "moderationStatus": "VISIBLE" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "rideDate cannot be in the future." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/reviews" \
  --header "Content-Type: application/json" \
  --data "{\"reviewerUserId\":5201,\"lineId\":6,\"rating\":4,\"reviewText\":\"Ride was stable with medium crowd.\",\"rideDate\":\"2026-04-10\"}"
```

- Notes:
  - 2026-04-12 (Copilot): New reviews start with moderation status `VISIBLE`.

## 7) Show Line Reviews By Line

- Title: Show Line Reviews By Line
- URL: `/api/v1/reviews?lineId=:lineId&includeHidden=:includeHidden`
- Method: `GET`
- URL Params:
  - Required:
    - `lineId=[integer > 0]`
      - Example: `lineId=6`
  - Optional:
    - `includeHidden=[boolean, default false]`
      - Example: `includeHidden=true`
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "id": 801, "lineId": 6, "rating": 4 } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reviews?lineId=6&includeHidden=false"
```

- Notes:
  - 2026-04-12 (Copilot): Hidden reviews are excluded unless `includeHidden=true`.

## 8) Show One Line Review

- Title: Show One Line Review
- URL: `/api/v1/reviews/:id`
- Method: `GET`
- URL Params:
  - Required:
    - `id=[integer > 0]`
      - Example: `id=801`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `{ "id": 801, "lineId": 6, "rating": 4 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "Line review not found: id=801" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reviews/801"
```

- Notes:
  - 2026-04-12 (Copilot): Path parameter `id` is validated as positive.

## 9) Show Line Reviews By Reviewer

- Title: Show Line Reviews By Reviewer
- URL: `/api/v1/reviews/reviewer/:reviewerUserId`
- Method: `GET`
- URL Params:
  - Required:
    - `reviewerUserId=[integer > 0]`
      - Example: `reviewerUserId=5201`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "id": 801, "reviewerUserId": 5201 } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reviews/reviewer/5201"
```

- Notes:
  - 2026-04-12 (Copilot): Sorted by `createdAt` descending.

## 10) Update One Review Moderation Status

- Title: Update One Review Moderation Status
- URL: `/api/v1/reviews/:id/moderation-status`
- Method: `PATCH`
- URL Params:
  - Required:
    - `id=[integer]`
      - Example: `id=801`
  - Optional: none
- Data Params:

```json
{
  "moderationStatus": "[enum, required, VISIBLE|HIDDEN]"
}
```

- Success Response:
  - Code: `200 OK`
  - Content: `{ "id": 801, "moderationStatus": "HIDDEN" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "Line review not found: id=801" }`
- Sample Call:

```bash
curl --request PATCH "http://localhost:8080/api/v1/reviews/801/moderation-status" \
  --header "Content-Type: application/json" \
  --data "{\"moderationStatus\":\"HIDDEN\"}"
```

- Notes:
  - 2026-04-12 (Copilot): Uses partial update semantics (`PATCH`).

## 11) Show Rating Summary For All Lines

- Title: Show Rating Summary For All Lines
- URL: `/api/v1/reviews/summary`
- Method: `GET`
- URL Params:
  - Required: none
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "lineId": 6, "averageRating": 4.25, "totalReviews": 12 } ]`
- Error Response:
  - Code: `500 INTERNAL SERVER ERROR`
  - Content: `{ "message": "Unexpected server error" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reviews/summary"
```

- Notes:
  - 2026-04-12 (Copilot): Includes only visible reviews.

## 12) Show Rating Summary For One Line

- Title: Show Rating Summary For One Line
- URL: `/api/v1/reviews/summary/:lineId`
- Method: `GET`
- URL Params:
  - Required:
    - `lineId=[integer > 0]`
      - Example: `lineId=6`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `{ "lineId": 6, "averageRating": 4.25, "totalReviews": 12 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/reviews/summary/6"
```

- Notes:
  - 2026-04-12 (Copilot): If no reviews exist, service returns `{ lineId, averageRating: 0.0, totalReviews: 0 }`.

## Documentation Artifacts

- API test evidence:
  - `docs/api-test-evidence/report-success.json`
  - `docs/api-test-evidence/report-failure.json`
  - `docs/api-test-evidence/review-success.json`
  - `docs/api-test-evidence/review-failure.json`
- Postman collection:
  - `docs/postman/feedbackservice.postman_collection.json`
- Screenshot folder:
  - `docs/postman-screenshots/`
