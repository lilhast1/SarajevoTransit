# Routing Service - Non-trivial API Routes

Scope: `routingservice` only (`/api/v1/**` from `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/controller/RoutingController.java`).

## What counts as non-trivial here

In this service, non-trivial behavior appears mainly as:

- Transactional service orchestration (`@Transactional` at class level)
  - Source: `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/service/RoutingCrudService.java:41`
- Multiple repository calls in one operation (fetch related entities, enforce consistency, then save)
- EntityGraph-based fetch optimization in repositories (`@EntityGraph`) to avoid N+1
- Domain consistency/uniqueness rules enforced in service code (not just DB auto-generated CRUD)

## Non-trivial routes

| Route | HTTP | Why non-trivial | Main repositories involved |
|---|---|---|---|
| `/api/v1/lines` | GET | Uses optional filters and `@EntityGraph` for `vehicleType`; read runs in transaction scope. | `LineRepository` |
| `/api/v1/lines` | POST | Checks external-id uniqueness, fetches `VehicleType`, then persists `Line` in one transaction. | `LineRepository`, `VehicleTypeRepository` |
| `/api/v1/lines/{id}` | PUT | Loads existing line, validates uniqueness conflict, loads `VehicleType`, updates entity. | `LineRepository`, `VehicleTypeRepository` |
| `/api/v1/directions` | GET | Filtered list with `@EntityGraph` to eagerly load related `line`. | `DirectionRepository` |
| `/api/v1/directions` | POST | Validates unique external id and resolves parent `Line` before save. | `DirectionRepository`, `LineRepository` |
| `/api/v1/directions/{id}` | PUT | Same orchestration as create: existing direction + unique check + line relation update. | `DirectionRepository`, `LineRepository` |
| `/api/v1/timetables` | GET | Uses `@EntityGraph(attributePaths={"line","direction"})` and ordered queries by departure time. | `TimetableRepository` |
| `/api/v1/timetables` | POST | Validates unique external id, loads `Direction` + `Line`, enforces line-direction consistency, then saves. | `TimetableRepository`, `DirectionRepository`, `LineRepository` |
| `/api/v1/timetables/{id}` | PUT | Same as create, plus loads existing timetable before update. | `TimetableRepository`, `DirectionRepository`, `LineRepository` |
| `/api/v1/directions/{directionId}/stations` | GET | Uses `@EntityGraph(direction,station)` and ordered relation retrieval (`stopSequence`). | `DirectionStationRepository` |
| `/api/v1/direction-stations` | POST | Resolves `Direction` + `Station`, enforces uniqueness (`direction+stopSequence`, `direction+station`) before save. | `DirectionRepository`, `StationRepository`, `DirectionStationRepository` |
| `/api/v1/direction-stations/{id}` | PUT | Loads relation, resolves referenced direction/station, re-validates uniqueness under update semantics. | `DirectionStationRepository`, `DirectionRepository`, `StationRepository` |
| `/api/v1/directions/{directionId}/route-points` | GET | Ordered retrieval of geometry points with `@EntityGraph(direction)`. | `RoutePointRepository` |
| `/api/v1/directions/{directionId}/geojson` | GET | Composes GeoJSON response by joining direction metadata + ordered route points (application-level aggregation). | `DirectionRepository`, `RoutePointRepository` |
| `/api/v1/route-points` | POST | Resolves direction, enforces sequence uniqueness per direction, then saves point. | `DirectionRepository`, `RoutePointRepository` |
| `/api/v1/route-points/{id}` | PUT | Loads point, resolves direction, validates uniqueness, updates coordinates/order. | `RoutePointRepository`, `DirectionRepository` |

## Requested patterns status

### 1) PATCH methods
- Not present in `routingservice` controller.

### 2) Pagination and sorting (`Pageable`, `Page`, `Sort`)
- Not present in `routingservice` API/repositories.

### 3) Custom non-derived repository queries (`@Query`)
- Present in repositories, mostly for import/bulk maintenance (`deactivateAll`, `deleteBy...In`), not as dedicated API endpoint features.
- Examples:
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/LineRepository.java:30`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/DirectionRepository.java:33`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/StationRepository.java:24`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/TimetableRepository.java:37`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/TimetableRepository.java:41`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/DirectionStationRepository.java:24`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/RoutePointRepository.java:21`

### 4) Batch insertion
- Present, but in import pipeline rather than HTTP endpoints.
- Chunked `saveAll` + `flush/clear` in:
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/importer/RoutingSnapshotImporter.java:637`
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/importer/RoutingSnapshotImporter.java:646`

### 5) EntityGraph usage
- Present and used by API-serving repository methods in `LineRepository`, `DirectionRepository`, `TimetableRepository`, `DirectionStationRepository`, and `RoutePointRepository`.

### 6) Transactional service methods with multiple repository calls
- Present and central to API behavior in `RoutingCrudService`.
- Class-level transaction boundary:
  - `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/service/RoutingCrudService.java:41`
