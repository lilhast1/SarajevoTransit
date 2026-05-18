#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "[INFO] SarajevoTransit shutdown"

echo "[INFO] Stopping Docker services..."
docker compose -f "$ROOT/docker-compose.yml" down 2>/dev/null || true
docker compose -f "$ROOT/docker-compose.otp.yml" down 2>/dev/null || true

echo "[INFO] Stopping Java processes for SarajevoTransit services..."

SERVICES=(
  configserver
  eurekaserver
  userservice
  feedbackservice
  notificationservice
  vehicleservice
  routingservice
  moneyman
  apigateway
  otpproxyservice
)

killed=0
for service in "${SERVICES[@]}"; do
  # Match any process (mvnw, mvn, java) whose command line references the service directory
  if pkill -f "$ROOT/$service" 2>/dev/null; then
    echo "[OK] Stopped $service"
    killed=$((killed + 1))
  else
    echo "[INFO] No running process found for $service"
  fi
done

echo "[INFO] Stopped $killed service(s)."
echo "[DONE] Shutdown complete."
