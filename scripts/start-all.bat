@echo off
setlocal

for %%I in ("%~dp0..") do set "ROOT_DIR=%%~fI"

echo [INFO] SarajevoTransit one-command startup
echo [INFO] Root: %ROOT_DIR%

if not exist "%ROOT_DIR%\userservice\mvnw.cmd" (
  echo [ERROR] userservice\mvnw.cmd not found.
  exit /b 1
)

if not exist "%ROOT_DIR%\otpproxyservice\mvnw.cmd" (
  echo [ERROR] otpproxyservice\mvnw.cmd not found.
  exit /b 1
)

if not exist "%ROOT_DIR%\routingservice\mvnw.cmd" (
  echo [ERROR] routingservice\mvnw.cmd not found.
  exit /b 1
)

echo [INFO] Starting Eureka server window...
start "SarajevoTransit Eureka" cmd /k "cd /d ""%ROOT_DIR%\userservice"" && call .\mvnw.cmd -f .\scripts\eureka-server\pom.xml spring-boot:run"

echo [INFO] Starting OTP container...
docker compose -f "%ROOT_DIR%\docker-compose.otp.yml" up -d otp
if errorlevel 1 (
  echo [ERROR] Failed to start OTP container.
  exit /b 1
)

echo [INFO] Starting OTP proxy window...
start "SarajevoTransit OTP Proxy" cmd /k "cd /d ""%ROOT_DIR%\otpproxyservice"" && call .\mvnw.cmd spring-boot:run"

echo [INFO] Starting routing service window...
start "SarajevoTransit Routing" cmd /k "cd /d ""%ROOT_DIR%\routingservice"" && call .\mvnw.cmd spring-boot:run"

echo [INFO] Waiting briefly before health checks...
timeout /t 20 /nobreak >nul

echo.
echo [INFO] Quick checks (may still be warming up):
curl -fsS "http://localhost:8761" >nul 2>&1 && echo [OK] Eureka: http://localhost:8761 || echo [WARN] Eureka not ready yet
curl -fsS "http://localhost:8080" >nul 2>&1 && echo [OK] OTP: http://localhost:8080 || echo [WARN] OTP not ready yet
curl -fsS "http://localhost:8082/api/v1/proxy/stops-count" >nul 2>&1 && echo [OK] OTP Proxy endpoint || echo [WARN] OTP Proxy endpoint not ready yet
curl -fsS "http://localhost:9999/api/v1/test/otp-stops-count" >nul 2>&1 && echo [OK] Routing discovery endpoint || echo [WARN] Routing endpoint not ready yet

echo.
echo [INFO] Startup commands sent.
echo [INFO] To stop everything run: scripts\stop-all.bat

endlocal
