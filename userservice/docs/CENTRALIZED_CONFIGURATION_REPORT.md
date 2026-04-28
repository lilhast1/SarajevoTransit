# Centralized Configuration Report

## Scope

Implementirana je centralizovana konfiguracija preko Spring Cloud Config Server-a za:

- userservice
- feedbackservice

Koristeni vodiči:

- https://www.baeldung.com/spring-cloud-configuration
- https://spring.io/guides/gs/centralized-configuration/
- https://docs.spring.io/spring-cloud-config/docs/current/reference/html/

## Implementacija

### 1) Config Server modul

Novi modul:

- `../../configserver`

Ključne stavke:

- `spring-cloud-config-server`
- `@EnableConfigServer`
- `native` backend za lokalni razvoj
- centralni fajlovi u `configserver/src/main/resources/central-config`

### 2) Config Client povezivanje

U oba servisa dodano:

- dependency: `spring-cloud-starter-config`
- `spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`
- `spring.profiles.default=dev`

### 3) Odvojeni profili

Centralni profile fajlovi:

- `userservice.yml`, `userservice-dev.yml`, `userservice-test.yml`
- `feedbackservice.yml`, `feedbackservice-dev.yml`, `feedbackservice-test.yml`

Praksa:

- `dev`: PostgreSQL + standardni runtime portovi
- `test`: H2 + discovery/eureka disabled

### 4) Test izolacija

Da testovi ne zavise od dostupnosti Config Server-a, u test properties oba servisa je dodano:

- `spring.cloud.config.enabled=false`

## Runbook

### Start Config Server

```powershell
.\mvnw.cmd -f ..\configserver\pom.xml spring-boot:run
```

### Provjera da server vraća konfiguraciju

- http://localhost:8888/userservice/dev
- http://localhost:8888/feedbackservice/dev

### Start userservice (dev)

```powershell
$env:CONFIG_SERVER_URL="http://localhost:8888"
.\mvnw.cmd spring-boot:run
```

### Start feedbackservice (dev)

```powershell
$env:CONFIG_SERVER_URL="http://localhost:8888"
.\mvnw.cmd spring-boot:run
```

### Pokretanje sa test profilom (opciono)

```powershell
$env:SPRING_PROFILES_ACTIVE="test"
```

## Verifikacija

Izvrseno nakon implementacije:

- configserver: `mvnw -f ..\configserver\pom.xml test` -> BUILD SUCCESS
- userservice: `mvnw test` -> 62 tests, 0 failures, 0 errors
- feedbackservice: `mvnw test` -> 72 tests, 0 failures, 0 errors
