#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OTP_DATA_DIR="${ROOT_DIR}/otp-data"
GTFS_TARGET="${OTP_DATA_DIR}/gtfs.zip"

ROUTING_BASE_URL="${ROUTING_BASE_URL:-http://localhost:9999}"
GTFS_EXPORT_PATH="${GTFS_EXPORT_PATH:-/api/v1/admin/gtfs/export}"
ADMIN_TOKEN="${ROUTING_GTFS_ADMIN_TOKEN:-}"
OSM_FILE="${OTP_OSM_FILE:-}"

if [[ -z "${ADMIN_TOKEN}" ]]; then
  echo "ERROR: ROUTING_GTFS_ADMIN_TOKEN is not set"
  exit 1
fi

mkdir -p "${OTP_DATA_DIR}"

if [[ -n "${OSM_FILE}" ]]; then
  if [[ ! -f "${OSM_FILE}" ]]; then
    echo "ERROR: OTP_OSM_FILE does not exist: ${OSM_FILE}"
    exit 1
  fi
  OSM_BASENAME="$(basename "${OSM_FILE}")"
  cp -f "${OSM_FILE}" "${OTP_DATA_DIR}/${OSM_BASENAME}"
fi

if ! ls "${OTP_DATA_DIR}"/*.osm.pbf >/dev/null 2>&1; then
  echo "ERROR: No .osm.pbf file found in ${OTP_DATA_DIR}"
  echo "Tip: set OTP_OSM_FILE=/absolute/path/to/sarajevo.osm.pbf"
  exit 1
fi

TMP_HEADERS="$(mktemp)"
TMP_ZIP="$(mktemp --suffix=.zip)"

echo "Downloading GTFS from routingservice..."
HTTP_CODE="$(curl -sS -X POST "${ROUTING_BASE_URL}${GTFS_EXPORT_PATH}" \
  -H "X-Admin-Token: ${ADMIN_TOKEN}" \
  -D "${TMP_HEADERS}" \
  -o "${TMP_ZIP}" \
  -w "%{http_code}")"

if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "ERROR: GTFS export failed with HTTP ${HTTP_CODE}"
  echo "--- Response headers ---"
  cat "${TMP_HEADERS}"
  echo "--- Response body (if text) ---"
  file "${TMP_ZIP}" || true
  head -c 2000 "${TMP_ZIP}" || true
  rm -f "${TMP_HEADERS}" "${TMP_ZIP}"
  exit 1
fi

mv -f "${TMP_ZIP}" "${GTFS_TARGET}"
rm -f "${TMP_HEADERS}"

echo "Building OTP graph..."
docker compose -f "${ROOT_DIR}/docker-compose.otp.yml" --profile tools run --rm otp-build

if docker ps --format '{{.Names}}' | grep -q '^otp$'; then
  echo "Restarting running OTP container..."
  docker compose -f "${ROOT_DIR}/docker-compose.otp.yml" restart otp
else
  echo "Starting OTP container..."
  docker compose -f "${ROOT_DIR}/docker-compose.otp.yml" up -d otp
fi

echo "Done. OTP is available at http://localhost:8080"
