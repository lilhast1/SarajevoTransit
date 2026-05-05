# GTFS Export Endpoint

The routing service now supports on-demand GTFS export.

## Endpoint

- Method: `POST`
- Path: `/api/v1/admin/gtfs/export`
- Header: `X-Admin-Token: <token>`
- Response: `application/zip` (GTFS feed)

## Configuration

Set admin token in `routingservice/src/main/resources/application.properties`:

```properties
routing.gtfs.admin-token=change-me
```

## Example request

```bash
curl -X POST "http://localhost:9999/api/v1/admin/gtfs/export" \
  -H "X-Admin-Token: change-me" \
  --output routing-gtfs.zip
```

## Files included in ZIP

- `agency.txt`
- `stops.txt`
- `routes.txt`
- `calendar.txt`
- `trips.txt`
- `stop_times.txt`
- `shapes.txt`
- `feed_info.txt`
