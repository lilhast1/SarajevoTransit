# OpenTripPlanner setup (callable rebuild)

This project can now rebuild OTP on demand using live GTFS exported by `routingservice`.

## Files added

- `docker-compose.otp.yml`
  - `otp`: serves planner API on `http://localhost:8080`
  - `otp-build`: one-shot graph build container (tools profile)
- `scripts/rebuild-otp.sh`
  - Calls `routingservice` GTFS export endpoint
  - Saves feed as `otp-data/gtfs.zip`
  - Builds OTP graph
  - Restarts (or starts) OTP

## Prerequisites

- Docker + Docker Compose
- Running `routingservice` on `http://localhost:9999`
- Valid `routing.gtfs.admin-token`
- OSM file for Sarajevo (`*.osm.pbf`)

## First-time setup

1. Put OSM file in `otp-data/` (example: `otp-data/sarajevo.osm.pbf`) or pass path via `OTP_OSM_FILE`.
2. Export token to environment:

```bash
export ROUTING_GTFS_ADMIN_TOKEN="change-me"
```

3. Run rebuild script:

```bash
bash scripts/rebuild-otp.sh
```

After completion, OTP is served on `http://localhost:8080`.

## Rebuild anytime (callable)

Run the same command whenever routing data changes:

```bash
bash scripts/rebuild-otp.sh
```

This gives you a manual/callable workflow instead of periodic jobs.

## Optional environment overrides

- `ROUTING_BASE_URL` (default: `http://localhost:9999`)
- `GTFS_EXPORT_PATH` (default: `/api/v1/admin/gtfs/export`)
- `ROUTING_GTFS_ADMIN_TOKEN` (required)
- `OTP_OSM_FILE` (optional absolute path to copy into `otp-data/`)

## Manual compose commands (without script)

Build graph:

```bash
docker compose -f docker-compose.otp.yml --profile tools run --rm otp-build
```

Start OTP:

```bash
docker compose -f docker-compose.otp.yml up -d otp
```

Restart OTP after a rebuild:

```bash
docker compose -f docker-compose.otp.yml restart otp
```
