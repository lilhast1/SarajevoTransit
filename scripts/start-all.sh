#!/usr/bin/env bash
set -euo pipefail

INCLUDE_OTP=false
for arg in "$@"; do
  case $arg in
    --include-otp) INCLUDE_OTP=true ;;
    *) echo "Unknown option: $arg"; exit 1 ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "[INFO] SarajevoTransit startup"
echo "[INFO] Root: $ROOT"

# ---------- load .env ----------
load_dotenv() {
  [ -f "$ROOT/.env" ] || return 0
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line#"${line%%[![:space:]]*}"}"
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* ]] || continue
    export "$line"
  done < "$ROOT/.env"
}
load_dotenv

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "[WARN] DB_PASSWORD is not set. Create .env from .env.example before starting."
fi

# Build env exports to forward into each new terminal window
ENV_PREFIX=""
for _var in DB_PASSWORD NOTIF_DB_PASSWORD OTP_HOST_PORT OTP_BASE_URL; do
  _val="${!_var:-}"
  [ -n "$_val" ] && ENV_PREFIX="${ENV_PREFIX}export ${_var}=$(printf '%q' "$_val"); "
done

# ---------- port helpers ----------
port_available() {
  ! nc -z localhost "$1" 2>/dev/null
}

get_random_port() {
  python3 -c "import socket; s=socket.socket(); s.bind(('',0)); p=s.getsockname()[1]; s.close(); print(p)"
}

get_fixed_port() {
  case $1 in
    configserver) echo 8888 ;;
    eurekaserver)  echo 8761 ;;
    apigateway)    echo 8080 ;;
    *)             echo "" ;;
  esac
}

get_preferred_port() {
  case $1 in
    userservice)         echo 8082 ;;
    feedbackservice)     echo 8091 ;;
    notificationservice) echo 8086 ;;
    vehicleservice)      echo 8083 ;;
    routingservice)      echo 9999 ;;
    moneyman)            echo 8081 ;;
    otpproxyservice)     echo 8082 ;;
    *)                   echo 0 ;;
  esac
}

RESERVED_PORTS=()

is_reserved() {
  local p=$1 r
  for r in ${RESERVED_PORTS[@]+"${RESERVED_PORTS[@]}"}; do
    [[ "$r" == "$p" ]] && return 0
  done
  return 1
}

# ---------- HTTP wait ----------
wait_for_http() {
  local url=$1 timeout=${2:-120}
  local deadline=$((SECONDS + timeout))
  echo "[INFO] Waiting for $url ..."
  while [ $SECONDS -lt $deadline ]; do
    curl -sf --max-time 5 "$url" > /dev/null 2>&1 && { echo "[OK] $url is up"; return 0; }
    sleep 2
  done
  echo "[ERROR] Timed out waiting for $url"
  exit 1
}

# ---------- terminal opener ----------
# Writes the command to a temp script to avoid shell-escaping issues when
# passing the command through terminal emulator argument strings.
open_terminal() {
  local title=$1 cmd=$2
  local tmp
  tmp=$(mktemp /tmp/st_XXXXXX.sh)
  printf '#!/usr/bin/env bash\n%s\nexec bash -l\n' "$cmd" > "$tmp"
  chmod +x "$tmp"

  if [[ "$OSTYPE" == "darwin"* ]]; then
    osascript -e "tell application \"Terminal\" to do script \"bash $tmp\""
  elif command -v gnome-terminal &>/dev/null; then
    gnome-terminal --title="$title" -- bash "$tmp" &
  elif command -v konsole &>/dev/null; then
    konsole --title "$title" -e bash "$tmp" &
  elif command -v xterm &>/dev/null; then
    xterm -title "$title" -e bash "$tmp" &
  elif command -v x-terminal-emulator &>/dev/null; then
    x-terminal-emulator -e bash "$tmp" &
  else
    echo "[WARN] No terminal emulator found, running '$title' in background. Log: /tmp/st_${title}.log"
    bash "$tmp" > "/tmp/st_${title}.log" 2>&1 &
  fi
}

# ---------- start service ----------
start_service_window() {
  local service=$1 port=$2
  local service_path="$ROOT/$service"

  [ -d "$service_path" ] || { echo "[ERROR] Service directory not found: $service_path"; exit 1; }

  local mvn_cmd
  if [ -f "$service_path/mvnw" ]; then
    chmod +x "$service_path/mvnw"
    mvn_cmd="./mvnw"
  elif command -v mvn &>/dev/null; then
    mvn_cmd="mvn"
  else
    echo "[ERROR] Cannot start '$service': missing mvnw and Maven (mvn) is not available on PATH."
    exit 1
  fi

  local run_cmd="${ENV_PREFIX}cd $(printf '%q' "$service_path") && $mvn_cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=$port"
  open_terminal "$service" "$run_cmd"
  echo "[OK] Started $service on port $port"
}

# ---------- services ----------
SERVICES=(
  configserver
  eurekaserver
  userservice
  feedbackservice
  notificationservice
  vehicleservice
  routingservice
  otpproxyservice
  moneyman
  apigateway
)

# ---------- pre-check ----------
echo "[INFO] Running pre-checks..."
MISSING=()
for service in "${SERVICES[@]}"; do
  sp="$ROOT/$service"
  if [ ! -d "$sp" ]; then
    MISSING+=("Missing service directory: $sp")
    continue
  fi
  if [ ! -f "$sp/mvnw" ] && ! command -v mvn &>/dev/null; then
    MISSING+=("Service '$service' has no mvnw and Maven (mvn) is not on PATH")
  fi
done

if [ ${#MISSING[@]} -gt 0 ]; then
  echo "[ERROR] Startup pre-check failed:"
  printf '  - %s\n' "${MISSING[@]}"
  exit 1
fi

# ---------- databases ----------
echo "[INFO] Starting databases with Docker Compose..."
docker compose -f "$ROOT/docker-compose.yml" up -d

# The postgres init script only runs on first volume creation.
# This ensures all required databases exist on every startup.
ensure_postgres_dbs() {
  local pg_dbs=(userdb feedbackdb sarajevo_transit_routing sarajevo_transit_vehicle sarajevo_transit_payment)
  echo "[INFO] Waiting for PostgreSQL to be ready..."
  local deadline=$((SECONDS + 60))
  until docker exec sarajevo-transit-postgres pg_isready -U postgres > /dev/null 2>&1; do
    [ $SECONDS -ge $deadline ] && { echo "[ERROR] PostgreSQL did not become ready in time"; exit 1; }
    sleep 2
  done
  echo "[INFO] Ensuring PostgreSQL databases exist..."
  for db in "${pg_dbs[@]}"; do
    exists=$(docker exec sarajevo-transit-postgres psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db'")
    if [ "$exists" != "1" ]; then
      docker exec sarajevo-transit-postgres psql -U postgres -c "CREATE DATABASE \"$db\";" > /dev/null
      echo "[OK] Created database: $db"
    else
      echo "[OK] Database already exists: $db"
    fi
  done
}
ensure_postgres_dbs

if [ "$INCLUDE_OTP" = true ]; then
  echo "[INFO] Starting OTP container..."
  docker compose -f "$ROOT/docker-compose.otp.yml" up -d
fi

# ---------- start loop ----------
SELECTED_PORTS_LIST=""

echo "[INFO] Starting Spring services in separate windows..."
for service in "${SERVICES[@]}"; do
  fixed=$(get_fixed_port "$service")

  if [ -n "$fixed" ]; then
    port=$fixed
    if is_reserved "$port" || ! port_available "$port"; then
      echo "[ERROR] Cannot start '$service' on locked port $port because the port is already in use."
      exit 1
    fi
  else
    preferred=$(get_preferred_port "$service")
    if [ "$preferred" -gt 0 ] && ! is_reserved "$preferred" && port_available "$preferred"; then
      port=$preferred
    else
      port=$(get_random_port)
      echo "[WARN] $service preferred port $preferred is busy; using random port $port"
    fi
  fi

  RESERVED_PORTS+=("$port")
  SELECTED_PORTS_LIST="$SELECTED_PORTS_LIST $service=$port"

  start_service_window "$service" "$port"
  sleep 2

  [[ "$service" == "configserver" ]] && wait_for_http "http://localhost:8888/actuator/health" 120
  [[ "$service" == "eurekaserver" ]]  && wait_for_http "http://localhost:8761/actuator/health" 180
done

# ---------- summary ----------
echo ""
echo "[DONE] Startup commands sent."
echo "[INFO] Selected ports:"
for entry in $SELECTED_PORTS_LIST; do
  echo "  - ${entry%=*} -> ${entry#*=}"
done
echo "[INFO] Eureka: http://localhost:8761"
echo "[INFO] Gateway Swagger: http://localhost:8080/swagger-ui/index.html"
echo "[INFO] To stop everything: bash ./scripts/stop-all.sh"
