# SarajevoTransit

Microservice-based public transit management system for Sarajevo.

---

## Running with Docker (recommended)

This is the easiest way to run the full stack on any operating system. All you need is **Docker Desktop** (or Docker Engine + Compose on Linux).

### Step 1 — Configure environment

```bash
cp .env.example .env
```

Open `.env` and set your values:

```
DB_PASSWORD=mysecretpassword       # PostgreSQL password
NOTIF_DB_PASSWORD=notif_pass       # MySQL password (notification service)
JP_API_TOKEN=                      # Bearer token for live vehicle positions (javniprevozks.ba)
OTP_HOST_PORT=18080                # Host port for OTP (only needed if using route planner)
```

> `.env` is gitignored and never committed.

---

### Step 2 — Generate RSA keys (first time only)

JWT tokens are signed with an RSA key pair. Run these commands once from the project root:

```bash
mkdir -p userservice/src/main/resources/keys apigateway/src/main/resources/keys
```
```bash
openssl genrsa -out tmp-rsa.pem 2048
```
```bash
openssl pkcs8 -topk8 -inform PEM -in tmp-rsa.pem -out userservice/src/main/resources/keys/rsa-private-key.pem -nocrypt
```
```bash
openssl rsa -in tmp-rsa.pem -pubout -out userservice/src/main/resources/keys/rsa-public-key.pem
```
```bash
cp userservice/src/main/resources/keys/rsa-public-key.pem apigateway/src/main/resources/keys/rsa-public-key.pem
```
```bash
rm tmp-rsa.pem
```

> The private key is gitignored. You must regenerate after every fresh clone.

---

### Step 3 — Build and start

The first build downloads all dependencies and compiles all services — this takes several minutes. Subsequent starts are instant.

```bash
docker compose -f docker-compose.full.yml up --build -d
```

---

### Step 4 — Import routing data (first time only)

Transit lines, stations and timetables must be imported once into the database:

```bash
docker compose -f docker-compose.full.yml stop routingservice
```
```bash
docker compose -f docker-compose.full.yml run --rm -e ROUTING_IMPORT_ENABLED=true routingservice
```
```bash
docker compose -f docker-compose.full.yml up -d routingservice
```

> Run this only once. The data persists in the PostgreSQL volume.

---

### Step 5 — Verify

| URL | What to check |
|-----|---------------|
| http://localhost:3000 | Frontend |
| http://localhost:8761 | Eureka — all services should show UP |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/actuator/health | Gateway health |
| http://localhost:15672 | RabbitMQ UI (guest / guest) |

---

### Daily usage

```bash
# Start everything (no rebuild, no downloads)
docker compose -f docker-compose.full.yml up -d

# Stop everything (data preserved)
docker compose -f docker-compose.full.yml down

# Wipe all database data and start fresh
docker compose -f docker-compose.full.yml down -v
```

---

## Route Planner / OTP (optional)

The route planner (best route between two points) requires OpenTripPlanner. This is optional — all other features work without it.

### Step 1 — Download a Sarajevo OSM map file (one time only)

```bash
mkdir -p otp-data
wget -O otp-data/sarajevo.osm.pbf https://download.geofabrik.de/europe/bosnia-herzegovina-latest.osm.pbf
```

### Step 2 — Build the OTP graph (one time only, ~5 min)

The full stack must be running and routing data must be imported (Step 4 above) before running this.

```bash
export ROUTING_GTFS_ADMIN_TOKEN=change-me
export ROUTING_BASE_URL=http://localhost:9999
bash scripts/rebuild-otp.sh
```

> After this completes, `otp-data/graph.obj` is saved on your machine and never needs to be rebuilt unless transit data changes.

### Step 3 — Start with OTP

```bash
docker compose -f docker-compose.full.yml --profile otp up -d
```

OTP will be available at http://localhost:18080

---

## Alternative: Manual startup (for development)

Use this if you want to run services outside Docker for faster development iteration.

### Prerequisites

- Java 21
- Maven
- Docker (for databases)

### 1. Start databases only

```bash
docker compose up -d
```

### 2. Start services in order

Open a separate terminal for each and run from its directory:

| # | Service | Directory | Port |
|---|---------|-----------|------|
| 1 | Config Server | `configserver/` | 8888 |
| 2 | Eureka | `eurekaserver/` | 8761 |
| 3 | User Service | `userservice/` | 8082 |
| 4 | Feedback Service | `feedbackservice/` | 8091 |
| 5 | Notification Service | `notificationservice/` | 8086 |
| 6 | Vehicle Service | `vehicleservice/` | 8083 |
| 7 | Routing Service | `routingservice/` | 9999 |
| 8 | Moneyman | `moneyman/` | 8081 |
| 9 | API Gateway | `apigateway/` | 8080 |

```bash
cd configserver && mvn spring-boot:run
```

Or use the convenience script:

```bash
# Linux / macOS
bash scripts/start-all.sh

# Windows
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

To stop:

```bash
bash scripts/stop-all.sh
```

### 3. Start frontend (dev mode)

```bash
cd frontend && npm install && npm run dev
```

Frontend runs at http://localhost:5173 and proxies all API calls to the gateway at `localhost:8080`.

### 4. Run frontend tests

```bash
cd frontend && npm test
```

---

## API Gateway Routes

All requests go through `http://localhost:8080`.

| Service | Path prefix |
|---------|-------------|
| User Service | `/api/users/**`, `/api/v1/users/**` |
| Feedback Service | `/api/v1/reviews/**`, `/api/v1/reports/**`, `/api/v1/workflows/**` |
| Notification Service | `/notifications/**`, `/subscriptions/**` |
| Vehicle Service | `/api/vehicles/**` |
| Routing Service | `/api/v1/lines/**`, `/api/v1/stations/**`, `/api/v1/directions/**`, `/api/v1/timetables/**`, `/api/v1/route-points/**`, `/api/v1/direction-stations/**` |
| Moneyman | `/api/finance/**`, `/api/payments/**` |
