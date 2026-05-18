# SarajevoTransit

Microservice-based public transit management system for Sarajevo.

## Prerequisites

- Java 21
- Maven
- Docker

## Running the Project

### 1. Configure Passwords

Copy the example environment file and set your passwords:

```bash
cp .env.example .env
```

Open `.env` and change the values if needed:

```
DB_PASSWORD=mysecretpassword   # PostgreSQL password (used by all services)
NOTIF_DB_PASSWORD=notif_pass   # MySQL password (notification service only)
OTP_HOST_PORT=18080            # Host port mapped to OTP container port 8080
OTP_BASE_URL=http://localhost:18080  # URL used by otp-proxy to call OTP
```

All services read `DB_PASSWORD` from the <environment. If you change it in `.env`, also export it in each terminal before running services:

```bash
export DB_PASSWORD=yourpassword
export NOTIF_DB_PASSWORD=yourpassword
```

> `.env` is gitignored and never committed. `.env.example` is the template that is committed.

---

### 2. Generate RSA Keys (first time only)

JWT tokens are signed with an RSA key pair. The private key lives only in the User Service; the API Gateway uses the public key to verify tokens.

Run this once from the project root:

```bash
mkdir -p userservice/src/main/resources/keys apigateway/src/main/resources/keys

openssl genrsa -out tmp-rsa.pem 2048
openssl pkcs8 -topk8 -inform PEM -in tmp-rsa.pem -out userservice/src/main/resources/keys/rsa-private-key.pem -nocrypt
openssl rsa -in tmp-rsa.pem -pubout -out userservice/src/main/resources/keys/rsa-public-key.pem
cp userservice/src/main/resources/keys/rsa-public-key.pem apigateway/src/main/resources/keys/rsa-public-key.pem
rm tmp-rsa.pem
```

> The private key is gitignored and never committed. You must regenerate it after a fresh clone.

---

### 3. Start Databases

From the project root:

```bash
docker compose up -d
```

This starts one PostgreSQL container (port 5432) with all 5 databases and one MySQL container (port 3312) for the notification service.

To stop:
```bash
docker compose down
```

To stop and wipe all data:
```bash
docker compose down -v
```

---

### 4. Start Services

#### Option A (recommended): one command

**Linux / macOS:**

```bash
bash scripts/start-all.sh
```

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

This starts:
- PostgreSQL + MySQL via Docker Compose
- Config Server, Eureka, all microservices, and API Gateway in separate terminal windows

If a service has no `mvnw` / `mvnw.cmd`, the script falls back to `mvn` only when Maven is installed globally. Otherwise it stops early with a clear pre-check error.
Ports are locked for Config Server (`8888`), Eureka (`8761`), and API Gateway (`8080`). For other services, the script tries the default port first and automatically picks a free random port when that port is already in use.

Optional flags:

```bash
# Linux / macOS
bash scripts/start-all.sh --include-otp
```

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1 -IncludeOtp
```

To stop everything:

```bash
# Linux / macOS
bash scripts/stop-all.sh
```

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
```

#### Option B: manual startup

Open a separate terminal for each service and run them **in this order**. Each service must be started from its own directory.

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
```bash
cd eurekaserver && mvn spring-boot:run
```
```bash
cd userservice && mvn spring-boot:run
```
```bash
cd feedbackservice && mvn spring-boot:run
```
```bash
cd notificationservice && mvn spring-boot:run
```
```bash
cd vehicleservice && mvn spring-boot:run
```
```bash
cd routingservice && mvn spring-boot:run
```
```bash
cd moneyman && mvn spring-boot:run
```
```bash
cd apigateway && mvn spring-boot:run
```

> Start the API Gateway last, after all other services are registered in Eureka.

---

### 5. Verify

- **Eureka dashboard** — http://localhost:8761 — all 7 services should show as UP
- **Swagger UI** — http://localhost:8080/swagger-ui/index.html — browse and test all APIs
- **Gateway health** — http://localhost:8080/actuator/health

---

### Seed Routing Data (first run only)

The routing service seeds vehicle types (bus, tram, trolleybus, minibus) automatically on every startup.

To load the full transit data (lines, stations, timetables) run once:

```bash
cd routingservice && mvn spring-boot:run -Dspring-boot.run.arguments="--routing.import.enabled=true"
```

> Run this only once. After the import completes, start the routing service normally on subsequent runs.

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
