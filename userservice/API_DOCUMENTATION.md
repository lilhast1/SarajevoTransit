# User Service API Documentation

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

Base API path used in examples: `/api/v1/users`

Backward-compatible aliases also exist under `/api/users`.

## 1) Show All Users

- Title: Show All Users
- URL: `/api/v1/users`
- Method: `GET`
- URL Params:
  - Required: none
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content:

```json
[
  {
    "id": 101,
    "fullName": "Lejla Music",
    "email": "lejla.music@sarajevotransit.ba",
    "loyaltyPointsBalance": 0,
    "preference": {
      "languageCode": "BS",
      "themeMode": "SYSTEM",
      "notificationChannel": "PUSH",
      "highContrastEnabled": false,
      "largeTextEnabled": false,
      "screenReaderEnabled": false,
      "updatedAt": "2026-04-12T21:55:10.514"
    },
    "createdAt": "2026-04-12T21:55:10.514",
    "updatedAt": "2026-04-12T21:55:10.514"
  }
]
```

- Error Response:
  - Code: `500 INTERNAL SERVER ERROR`
  - Content:

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Unexpected server error"
}
```

- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users"
```

- Notes:
  - 2026-04-12 (Copilot): This endpoint returns all users without pagination.

## 2) Show One User

- Title: Show One User
- URL: `/api/v1/users/:userId`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content:

```json
{
  "id": 101,
  "fullName": "Lejla Music",
  "email": "lejla.music@sarajevotransit.ba",
  "loyaltyPointsBalance": 0
}
```

- Error Response:
  - Code: `400 BAD REQUEST`
  - Content:

```json
{
  "message": "Validation failed",
  "validationErrors": ["userId: must be greater than 0"]
}
```

- OR

- Code: `404 NOT FOUND`
- Content:

```json
{
  "message": "User with id 101 not found."
}
```

- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101"
```

- Notes:
  - 2026-04-12 (Copilot): Returns one `UserProfileResponse` object.

## 3) Create One User

- Title: Create One User
- URL: `/api/v1/users`
- Method: `POST`
- URL Params:
  - Required: none
  - Optional: none
- Data Params:

```json
{
  "fullName": "[string, required, not blank]",
  "email": "[string, required, valid email]",
  "password": "[string, required, min 8, max 128]",
  "languageCode": "[enum optional: BS|SR|HR|EN]",
  "themeMode": "[enum optional: LIGHT|DARK|SYSTEM]",
  "notificationChannel": "[enum optional: PUSH|SMS|EMAIL]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content:

```json
{
  "id": 101,
  "fullName": "Lejla Music",
  "email": "lejla.music@sarajevotransit.ba"
}
```

- Error Response:
  - Code: `400 BAD REQUEST`
  - Content:

```json
{
  "message": "Validation failed",
  "validationErrors": [
    "fullName: Full name is required",
    "email: Email format is invalid",
    "password: Password must be between 8 and 128 characters"
  ]
}
```

- OR

- Code: `409 CONFLICT`
- Content:

```json
{
  "message": "A user with this email already exists."
}
```

- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/users" \
  --header "Content-Type: application/json" \
  --data "{\"fullName\":\"Lejla Music\",\"email\":\"lejla.music@sarajevotransit.ba\",\"password\":\"StrongPass123\"}"
```

- Notes:
  - 2026-04-12 (Copilot): Email is normalized to lowercase before save.

## 4) Update One User Profile

- Title: Update One User Profile
- URL: `/api/v1/users/:userId`
- Method: `PUT`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "fullName": "[string, required, not blank]",
  "email": "[string, required, valid email]"
}
```

- Success Response:
  - Code: `200 OK`
  - Content: `{ "id": 101, "fullName": "Lejla Music", "email": "lejla.music@sarajevotransit.ba" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
  - OR
  - Code: `409 CONFLICT`
  - Content: `{ "message": "Email is already used by another user." }`
- Sample Call:

```bash
curl --request PUT "http://localhost:8080/api/v1/users/101" \
  --header "Content-Type: application/json" \
  --data "{\"fullName\":\"Lejla Updated\",\"email\":\"lejla.updated@sarajevotransit.ba\"}"
```

- Notes:
  - 2026-04-12 (Copilot): This endpoint updates only profile identity fields.

## 5) Update One User Password

- Title: Update One User Password
- URL: `/api/v1/users/:userId/password`
- Method: `PUT`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "newPassword": "[string, required, min 8, max 128]"
}
```

- Success Response:
  - Code: `204 NO CONTENT`
  - Content: none
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request PUT "http://localhost:8080/api/v1/users/101/password" \
  --header "Content-Type: application/json" \
  --data "{\"newPassword\":\"EvenStronger123\"}"
```

- Notes:
  - 2026-04-12 (Copilot): Password is hashed before persistence.

## 6) Show One User Preference

- Title: Show One User Preference
- URL: `/api/v1/users/:userId/preferences`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `{ "languageCode": "BS", "themeMode": "SYSTEM", "notificationChannel": "PUSH" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/preferences"
```

- Notes:
  - 2026-04-12 (Copilot): Preference may be null for legacy records without profile defaults.

## 7) Update One User Preference

- Title: Update One User Preference
- URL: `/api/v1/users/:userId/preferences`
- Method: `PUT`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "languageCode": "[enum required: BS|SR|HR|EN]",
  "themeMode": "[enum required: LIGHT|DARK|SYSTEM]",
  "notificationChannel": "[enum required: PUSH|SMS|EMAIL]",
  "highContrastEnabled": "[boolean]",
  "largeTextEnabled": "[boolean]",
  "screenReaderEnabled": "[boolean]"
}
```

- Success Response:
  - Code: `200 OK`
  - Content: `{ "languageCode": "EN", "themeMode": "DARK", "notificationChannel": "EMAIL" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request PUT "http://localhost:8080/api/v1/users/101/preferences" \
  --header "Content-Type: application/json" \
  --data "{\"languageCode\":\"EN\",\"themeMode\":\"DARK\",\"notificationChannel\":\"EMAIL\",\"highContrastEnabled\":false,\"largeTextEnabled\":false,\"screenReaderEnabled\":false}"
```

- Notes:
  - 2026-04-12 (Copilot): Endpoint is idempotent and overwrites preference values.

## 8) Create One Travel History Entry

- Title: Create One Travel History Entry
- URL: `/api/v1/users/:userId/travel-history`
- Method: `POST`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "lineCode": "[string, required, max 40]",
  "fromStop": "[string, required]",
  "toStop": "[string, required]",
  "traveledAt": "[datetime optional, ISO-8601]",
  "durationMinutes": "[integer, required, min 1]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `{ "id": 9001, "lineCode": "TRAM-1", "durationMinutes": 12 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/users/101/travel-history" \
  --header "Content-Type: application/json" \
  --data "{\"lineCode\":\"TRAM-1\",\"fromStop\":\"Skenderija\",\"toStop\":\"Marijin Dvor\",\"durationMinutes\":12}"
```

- Notes:
  - 2026-04-12 (Copilot): `traveledAt` defaults to now when omitted.

## 9) Show All Travel History Entries for One User

- Title: Show All Travel History Entries for One User
- URL: `/api/v1/users/:userId/travel-history`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "id": 9001, "lineCode": "TRAM-1" } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/travel-history"
```

- Notes:
  - 2026-04-12 (Copilot): Results are ordered by `traveledAt` descending.

## 10) Create One Ticket Purchase Entry

- Title: Create One Ticket Purchase Entry
- URL: `/api/v1/users/:userId/ticket-purchases`
- Method: `POST`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "ticketType": "[enum required: SINGLE|DAILY|WEEKLY|MONTHLY]",
  "amount": "[decimal, required, > 0]",
  "paymentMethod": "[string, required]",
  "externalTransactionId": "[string, required]",
  "lineCode": "[string optional, max 40]",
  "purchasedAt": "[datetime optional, ISO-8601]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `{ "id": 7001, "ticketType": "MONTHLY", "amount": 53.00 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/users/101/ticket-purchases" \
  --header "Content-Type: application/json" \
  --data "{\"ticketType\":\"MONTHLY\",\"amount\":53.00,\"paymentMethod\":\"CARD\",\"externalTransactionId\":\"TXN-101\",\"lineCode\":\"TRAM-3\"}"
```

- Notes:
  - 2026-04-12 (Copilot): `purchasedAt` defaults to now when omitted.

## 11) Show All Ticket Purchase Entries for One User

- Title: Show All Ticket Purchase Entries for One User
- URL: `/api/v1/users/:userId/ticket-purchases`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `[ { "id": 7001, "ticketType": "MONTHLY", "amount": 53.00 } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/ticket-purchases"
```

- Notes:
  - 2026-04-12 (Copilot): Results are ordered by `purchasedAt` descending.

## 12) Show One User Summary

- Title: Show One User Summary
- URL: `/api/v1/users/:userId/summary`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content:

```json
{
  "profile": { "id": 101, "fullName": "Lejla Music" },
  "travelHistory": [],
  "ticketPurchases": [],
  "loyaltyTransactions": [],
  "personalizedLineSuggestions": []
}
```

- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/summary"
```

- Notes:
  - 2026-04-12 (Copilot): Aggregates local user, travel, purchase, and loyalty data.

## 13) Show Personalized Line Suggestions

- Title: Show Personalized Line Suggestions
- URL: `/api/v1/users/:userId/suggestions?limit=:limit`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional:
    - `limit=[integer 1..10, default 3]`
      - Example: `limit=5`
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content:

```json
["TRAM-3", "BUS-31E", "TROL-103"]
```

- Error Response:
  - Code: `400 BAD REQUEST`
  - Content:

```json
{
  "message": "Validation failed",
  "validationErrors": ["limit: must be greater than or equal to 1"]
}
```

- OR

- Code: `404 NOT FOUND`
- Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/suggestions?limit=3"
```

- Notes:
  - 2026-04-12 (Copilot): Scores suggestions from local travel history usage.

## 14) Earn Loyalty Points

- Title: Earn Loyalty Points
- URL: `/api/v1/users/:userId/loyalty/earn`
- Method: `POST`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "points": "[integer, required, min 1]",
  "description": "[string, required]",
  "referenceType": "[string, required]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `{ "userId": 101, "currentBalance": 50 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/users/101/loyalty/earn" \
  --header "Content-Type: application/json" \
  --data "{\"points\":50,\"description\":\"Ticket purchase bonus\",\"referenceType\":\"ticket_purchase\"}"
```

- Notes:
  - 2026-04-12 (Copilot): Creates a loyalty transaction and updates wallet total.

## 15) Redeem Loyalty Points

- Title: Redeem Loyalty Points
- URL: `/api/v1/users/:userId/loyalty/redeem`
- Method: `POST`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
{
  "points": "[integer, required, min 1]",
  "description": "[string, required]",
  "referenceType": "[string, required]"
}
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `{ "userId": 101, "currentBalance": 20 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content:

```json
{
  "message": "User does not have enough loyalty points for redemption."
}
```

- OR

- Code: `404 NOT FOUND`
- Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/users/101/loyalty/redeem" \
  --header "Content-Type: application/json" \
  --data "{\"points\":30,\"description\":\"Ride discount\",\"referenceType\":\"discount\"}"
```

- Notes:
  - 2026-04-12 (Copilot): Fails with 400 when requested points exceed current balance.

## 16) Show Loyalty Balance

- Title: Show Loyalty Balance
- URL: `/api/v1/users/:userId/loyalty/balance`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content: `{ "userId": 101, "currentBalance": 50 }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/loyalty/balance"
```

- Notes:
  - 2026-04-12 (Copilot): Returns aggregate points from digital wallet.

## 17) Show Loyalty Transactions

- Title: Show Loyalty Transactions
- URL: `/api/v1/users/:userId/loyalty/transactions`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content:

```json
[
  {
    "id": 4001,
    "transactionType": "EARN",
    "points": 50,
    "description": "Ticket purchase bonus",
    "referenceType": "ticket_purchase",
    "createdAt": "2026-04-12T22:10:11.101"
  }
]
```

- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/loyalty/transactions"
```

- Notes:
  - 2026-04-12 (Copilot): Returned list is ordered by `createdAt` descending.

## 18) Patch User Profile (JSON Patch)

- Title: Patch User Profile (JSON Patch)
- URL: `/api/v1/users/:userId`
- Method: `PATCH`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Headers:
  - `Content-Type: application/json-patch+json`
- Data Params:

```json
[
  { "op": "replace", "path": "/fullName", "value": "Lejla Updated" },
  {
    "op": "replace",
    "path": "/email",
    "value": "lejla.updated@sarajevotransit.ba"
  }
]
```

- Success Response:
  - Code: `200 OK`
  - Content: `{ "id": 101, "fullName": "Lejla Updated", "email": "lejla.updated@sarajevotransit.ba" }`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request PATCH "http://localhost:8080/api/v1/users/101" \
  --header "Content-Type: application/json-patch+json" \
  --data '[{"op":"replace","path":"/fullName","value":"Lejla Updated"}]'
```

- Notes:
  - 2026-04-15 (Copilot): Supported patch paths are `/fullName` and `/email`; unsupported operations/paths return 400.

## 19) Batch Create Travel History Entries

- Title: Batch Create Travel History Entries
- URL: `/api/v1/users/:userId/travel-history/batch`
- Method: `POST`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params:

```json
[
  {
    "lineCode": "TRAM-3",
    "fromStop": "Skenderija",
    "toStop": "Bascarsija",
    "durationMinutes": 18
  },
  {
    "lineCode": "BUS-31E",
    "fromStop": "Nedzarici",
    "toStop": "Dobrinja",
    "durationMinutes": 22
  }
]
```

- Success Response:
  - Code: `201 CREATED`
  - Content: `[ { "id": 9001, "lineCode": "TRAM-3" }, { "id": 9002, "lineCode": "BUS-31E" } ]`
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request POST "http://localhost:8080/api/v1/users/101/travel-history/batch" \
  --header "Content-Type: application/json" \
  --data '[{"lineCode":"TRAM-3","fromStop":"Skenderija","toStop":"Bascarsija","durationMinutes":18}]'
```

- Notes:
  - 2026-04-15 (Copilot): Batch operation is transactional and rolls back all entries on any validation/persistence error.

## 20) Ticket Purchase Statistics (Custom Query)

- Title: Ticket Purchase Statistics
- URL: `/api/v1/users/:userId/ticket-purchases/stats`
- Method: `GET`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
      - Example: `userId=101`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `200 OK`
  - Content:

```json
[
  { "ticketType": "MONTHLY", "purchaseCount": 2, "totalAmount": 106.0 },
  { "ticketType": "DAILY", "purchaseCount": 1, "totalAmount": 2.0 }
]
```

- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "User with id 101 not found." }`
- Sample Call:

```bash
curl --request GET "http://localhost:8080/api/v1/users/101/ticket-purchases/stats"
```

- Notes:
  - 2026-04-15 (Copilot): Backed by a JPQL aggregate query grouped by `ticketType` (not repository method derivation).

## 21) Delete Travel History Entry

- Title: Delete Travel History Entry
- URL: `/api/v1/users/:userId/travel-history/:entryId`
- Method: `DELETE`
- URL Params:
  - Required:
    - `userId=[integer > 0]`
    - `entryId=[integer > 0]`
      - Example: `entryId=9001`
  - Optional: none
- Data Params: none
- Success Response:
  - Code: `204 NO CONTENT`
  - Content: none
- Error Response:
  - Code: `400 BAD REQUEST`
  - Content: `{ "message": "Validation failed" }`
  - OR
  - Code: `404 NOT FOUND`
  - Content: `{ "message": "Travel history entry with id 9001 not found for user 101." }`
- Sample Call:

```bash
curl --request DELETE "http://localhost:8080/api/v1/users/101/travel-history/9001"
```

- Notes:
  - 2026-04-15 (Copilot): Entry ownership is checked by custom query before delete.

## Documentation Artifacts

- API test report: `API_TEST_REPORT.md`
- Postman collection: `docs/postman/userservice.postman_collection.json`
- Request-response captures: `docs/api-test-evidence/*.json`
- Screenshot placeholders and names: `docs/postman-screenshots/README.md`
