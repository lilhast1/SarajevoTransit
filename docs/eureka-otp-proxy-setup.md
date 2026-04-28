# Eureka + OTP Proxy setup

This repository now uses the Eureka server from:

- `userservice/scripts/eureka-server` (port `8761`)

And includes:

- `otpproxyservice` (Eureka client + synchronous OTP proxy, port `8082`)

`routingservice` is now also a Eureka client and includes a test route that forwards synchronously to `otpproxyservice`.

## Endpoints

- OTP proxy endpoint:
  - `GET http://localhost:8082/api/v1/proxy/stops-count`
- Routing test endpoint (via Eureka discovery):
  - `GET http://localhost:9999/api/v1/test/otp-stops-count`

## Run order

1. Start `userservice/scripts/eureka-server`.
2. Start OTP container (`docker-compose.otp.yml`).
3. Start `otpproxyservice`.
4. Start `routingservice`.

### Start commands (Windows)

```bat
cd userservice
.\mvnw.cmd -f .\scripts\eureka-server\pom.xml spring-boot:run
```

In separate terminals:

```bat
cd otpproxyservice
.\mvnw.cmd spring-boot:run
```

```bat
cd routingservice
.\mvnw.cmd spring-boot:run
```

## Manual test

```bash
curl "http://localhost:8082/api/v1/proxy/stops-count"
curl "http://localhost:9999/api/v1/test/otp-stops-count"
```

Both should return a JSON object like:

```json
{"count":1138,"source":"otp-proxy"}
```
