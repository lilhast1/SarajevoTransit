#!/usr/bin/env bash
# Sends 100 requests to notificationservice in two modes via the benchmark endpoint:
#   direct  - calls a fixed instance (port 8086) directly, no Eureka involved
#   lb      - goes through Spring Cloud LoadBalancer (Eureka round-robin across instances)
# Both modes hit GET /api/v1/discovery/ping?mode=<mode> on instance 1 (port 8086),
# which internally routes to the chosen target and returns which instance responded.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8086}"
PING_ENDPOINT="/api/v1/discovery/ping"
ITERATIONS=100
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT="$SCRIPT_DIR/../docs/benchmark-result.json"

mkdir -p "$(dirname "$OUTPUT")"

run_mode() {
    local mode="$1"
    local url="$BASE_URL$PING_ENDPOINT?mode=$mode"
    declare -A hits=()
    declare -a durations=()
    local success=0 failure=0

    echo "Running $ITERATIONS requests [mode=$mode] -> $url" >&2

    for ((i = 1; i <= ITERATIONS; i++)); do
        start_ns=$(date +%s%N)
        response=$(curl -s --max-time 10 "$url" 2>/dev/null) || { ((failure++)); continue; }
        end_ns=$(date +%s%N)

        duration_ms=$(( (end_ns - start_ns) / 1000000 ))
        durations+=("$duration_ms")

        instance_id=$(echo "$response" | grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4)
        [[ -z "$instance_id" ]] && instance_id="unknown"

        hits["$instance_id"]=$(( ${hits["$instance_id"]:-0} + 1 ))
        ((success++))
    done

    local total=0 min=999999 max=0
    for d in "${durations[@]}"; do
        total=$((total + d))
        (( d < min )) && min=$d
        (( d > max )) && max=$d
    done
    local count=${#durations[@]}
    local avg=0
    (( count > 0 )) && avg=$((total / count))

    echo "  success=$success  failure=$failure  min=${min}ms  avg=${avg}ms  max=${max}ms" >&2
    echo "  Instance distribution:" >&2
    for inst in "${!hits[@]}"; do
        echo "    $inst -> ${hits[$inst]} requests" >&2
    done
    echo "" >&2

    local dist_json=""
    for inst in "${!hits[@]}"; do
        dist_json+="\"$inst\": ${hits[$inst]}, "
    done
    dist_json="${dist_json%, }"

    printf '{"mode":"%s","successCount":%d,"failureCount":%d,"minMs":%d,"avgMs":%d,"maxMs":%d,"instanceDistribution":{%s}}' \
        "$mode" "$success" "$failure" "$min" "$avg" "$max" "$dist_json"
}

direct_json=$(run_mode "direct")
lb_json=$(run_mode "lb")

cat > "$OUTPUT" <<EOF
{
  "generatedAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "baseUrl": "$BASE_URL",
  "iterations": $ITERATIONS,
  "direct": $direct_json,
  "loadBalanced": $lb_json
}
EOF

echo "Results saved to $OUTPUT"
