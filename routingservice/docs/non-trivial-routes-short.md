# Routing Service - Non-trivial Routes (Short)

Source controller: `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/controller/RoutingController.java`

## Non-trivial API routes

- `GET /api/v1/lines`
  - Optional filtering + `@EntityGraph` (`vehicleType`) to avoid N+1.
- `POST /api/v1/lines`
  - Uniqueness check (`externalId`) + fetch `VehicleType` + save in one transaction.
- `PUT /api/v1/lines/{id}`
  - Load existing line + uniqueness check + relation update.

- `GET /api/v1/directions`
  - Filtered listing with `@EntityGraph` (`line`).
- `POST /api/v1/directions`
  - Uniqueness check + parent `Line` resolution.
- `PUT /api/v1/directions/{id}`
  - Existing entity update + uniqueness validation + relation update.

- `GET /api/v1/timetables`
  - Ordered reads with `@EntityGraph` (`line`, `direction`).
- `POST /api/v1/timetables`
  - Multi-entity validation: `Direction`, `Line`, and line-direction consistency.
- `PUT /api/v1/timetables/{id}`
  - Same as create + existing timetable load.

- `GET /api/v1/directions/{directionId}/stations`
  - Ordered relation read (`stopSequence`) with `@EntityGraph` (`direction`, `station`).
- `POST /api/v1/direction-stations`
  - Enforces uniqueness rules (`direction+stopSequence`, `direction+station`).
- `PUT /api/v1/direction-stations/{id}`
  - Update with same uniqueness and relation checks.

- `GET /api/v1/directions/{directionId}/route-points`
  - Ordered geometry read with `@EntityGraph` (`direction`).
- `GET /api/v1/directions/{directionId}/geojson`
  - Application-level aggregation into GeoJSON from direction + route points.
- `POST /api/v1/route-points`
  - Direction resolution + per-direction sequence uniqueness check.
- `PUT /api/v1/route-points/{id}`
  - Existing point update + sequence uniqueness validation.

## Requested feature status (routingservice only)

- PATCH endpoints: **No**
- Pagination/sorting (`Pageable`, `Page`, `Sort`): **No**
- Custom `@Query` repository methods: **Yes** (bulk delete/deactivate patterns)
- Batch insertion: **Yes** (import pipeline, chunked `saveAll` + `flush/clear`)
- `@EntityGraph`: **Yes** (multiple repositories)
- Transactional multi-repository service logic: **Yes** (`RoutingCrudService`, class-level `@Transactional`)

## Key references

- Transactions: `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/service/RoutingCrudService.java:41`
- Import batching: `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/importer/RoutingSnapshotImporter.java:637`
- Import batching flush/clear: `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/importer/RoutingSnapshotImporter.java:646`
- Repository custom queries: `routingservice/src/main/java/ba/unsa/etf/pnwt/routingservice/repository/TimetableRepository.java:37`
