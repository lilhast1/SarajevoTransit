# Service Discovery and Load Balancing Report

## Scope

This report documents Eureka-based service discovery and client-side load balancing between:

- userservice (client)
- feedbackservice (target service with two running instances)

References used for guidance:

- https://spring.io/guides/gs/service-registration-and-discovery/
- https://www.javatpoint.com/client-side-load-balancing-with-ribbon
- https://cloud.spring.io/spring-cloud-netflix/multi/multi_spring-cloud-ribbon.html

Note: Ribbon is a legacy Netflix client. On Spring Boot 4 / Spring Cloud 2025.x, the supported replacement is Spring Cloud LoadBalancer. The implementation follows the same client-side load-balancing principle with Eureka-based instance discovery.

## Implemented Components

- Eureka client registration in userservice and feedbackservice
- Actuator health checks exposed and connected to Eureka health status
- feedbackservice instance endpoint for identifying serving instance:
  - GET /api/v1/discovery/instance
- userservice benchmark endpoint:
  - GET /api/v1/discovery/feedback/ping?mode=direct|lb
- Benchmark script:
  - userservice/scripts/benchmark-discovery-loadbalancing.ps1

## Health Check Verification

Check these endpoints after startup:

- userservice health:
  - http://localhost:8080/actuator/health
- feedbackservice instance 1 health:
  - http://localhost:8091/actuator/health
- feedbackservice instance 2 health:
  - http://localhost:8092/actuator/health
- Eureka dashboard:
  - http://localhost:8761/

Expected:

- userservice and two feedbackservice instances appear as UP in Eureka
- /actuator/health returns status UP for all running instances

## Benchmark Runbook (100 Requests)

### 1) Start Eureka server

Start local Eureka server launcher:

```powershell
.\mvnw.cmd -f .\scripts\eureka-server\pom.xml spring-boot:run
```

### 2) Start feedbackservice instances

Terminal A:

```powershell
$env:SERVER_PORT="8091"
.\mvnw.cmd spring-boot:run
```

Terminal B:

```powershell
$env:SERVER_PORT="8092"
.\mvnw.cmd spring-boot:run
```

### 3) Start userservice

Terminal C:

```powershell
.\mvnw.cmd spring-boot:run
```

### 4) Run benchmark script

Terminal D:

```powershell
.\scripts\benchmark-discovery-loadbalancing.ps1 -Iterations 100
```

Generated result file:

- userservice/docs/api-test-evidence/discovery-loadbalancing-benchmark.json

## Result Summary (Measured)

Source artifact:

- userservice/docs/api-test-evidence/discovery-loadbalancing-benchmark.json
- generatedAt: 2026-04-16T21:08:31.1701664+02:00

Instance mapping used in the table:

- Instance A: feedbackservice:localhost:8091
- Instance B: feedbackservice:localhost:8092

| Mode                       | Total Requests | Success | Failure | Instance A Count | Instance B Count | Avg Request Time (ms) | P95 Request Time (ms) | Total Duration (ms) |
| -------------------------- | -------------: | ------: | ------: | ---------------: | ---------------: | --------------------: | --------------------: | ------------------: |
| direct (no LB)             |            100 |     100 |       0 |              100 |                0 |                 13.11 |                 10.71 |             1348.13 |
| lb (service-id via Eureka) |            100 |     100 |       0 |               50 |               50 |                 10.62 |                  8.97 |             1064.82 |

Interpretation:

- direct mode concentrated all successful traffic on a single fixed instance (8091)
- lb mode distributed traffic evenly across the two feedbackservice instances (50/50)
- in this run, the lb mode had lower overall request latency (avg and p95) and lower total duration
