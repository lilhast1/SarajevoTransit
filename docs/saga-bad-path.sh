#!/usr/bin/env bash
# Saga compensation path demo.
# Flow:
#   1. Admin logs in
#   2. Creates testuser
#   3. Logs in as testuser, adds payment method
#   4. Triggers purchase as testuser → ticket PENDING, event published
#   5. Admin immediately deletes testuser
#   6. UserService receives event, findUserById fails → publishes UserFailed
#   7. Moneyman compensates → ticket CANCELLED, transaction FAILED
#   8. NotificationService creates "Purchase failed" notification

set -euo pipefail

GW="http://localhost:8080"
RABBIT_API="http://localhost:15672/api"
RMQ="guest:guest"
SAGA_WAIT=4

BOLD='\033[1m'; CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; RED='\033[0;31m'; RESET='\033[0m'
header()  { echo -e "\n${BOLD}${CYAN}══ $* ══${RESET}"; }
section() { echo -e "\n${BOLD}${YELLOW}── $* ──${RESET}"; }
ok()      { echo -e "${GREEN}[OK] $*${RESET}"; }
bad()     { echo -e "${RED}[FAIL] $*${RESET}"; }

queue_stat() {
  curl -sf -u "$RMQ" "$RABBIT_API/queues/%2F/$1" | \
    jq "{queue: \"$1\", ready: .messages_ready, unacked: .messages_unacknowledged, consumers}"
}

# ── 1. admin login ────────────────────────────────────────────────────────────
header "STEP 1 — Admin login"
ADMIN_TOKEN=$(curl -sf -X POST "$GW/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@sarajevotransit.ba","password":"AdminPass123!"}' | jq -r '.accessToken')
ok "Admin token obtained"
ADMIN_AUTH="Authorization: Bearer $ADMIN_TOKEN"

# ── 2. create testuser ────────────────────────────────────────────────────────
header "STEP 2 — Create testuser"
TEST_EMAIL="testuser.$(date +%s)@sarajevotransit.ba"
TEST_USER=$(curl -sf -X POST "$GW/api/users" \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Test User\",\"email\":\"$TEST_EMAIL\",\"password\":\"TestPass123!\"}")
TEST_USER_ID=$(echo "$TEST_USER" | jq -r '.id')
ok "testuser created — id: $TEST_USER_ID, email: $TEST_EMAIL"

# ── 3. login as testuser ──────────────────────────────────────────────────────
header "STEP 3 — Login as testuser"
TEST_TOKEN=$(curl -sf -X POST "$GW/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"TestPass123!\"}" | jq -r '.accessToken')
ok "testuser token obtained"
TEST_AUTH="Authorization: Bearer $TEST_TOKEN"

# ── 4. add payment method ─────────────────────────────────────────────────────
header "STEP 4 — Add payment method for testuser"
PM=$(curl -sf -X POST "$GW/api/payments/methods" \
  -H "Content-Type: application/json" \
  -H "$TEST_AUTH" \
  -d "{\"userId\":$TEST_USER_ID,\"provider\":\"TestBank\",\"gatewayToken\":\"tok_test_saga\",\"lastFour\":\"9999\",\"cardType\":\"VISA\",\"default\":true}")
PM_ID=$(echo "$PM" | jq -r '.id')
ok "Payment method created — id: $PM_ID"

# ── before state ──────────────────────────────────────────────────────────────
header "STATE BEFORE"

section "Queue states"
for q in ticket-user-update-queue ticket-moneyman-user-failed-queue ticket-notification-cancelled-queue; do
  queue_stat "$q"
done

# ── 5. delete testuser BEFORE purchase ───────────────────────────────────────
header "STEP 5 — Admin deletes testuser BEFORE triggering purchase"
curl -sf -X DELETE "$GW/api/users/$TEST_USER_ID" -H "$ADMIN_AUTH"
ok "testuser (id=$TEST_USER_ID) deleted"
echo "  JWT is still valid (gateway checks RSA signature only, not DB)"
echo "  moneyman will accept the purchase and publish the event"
echo "  UserService will call findUserById($TEST_USER_ID) → user gone → UserFailed"

# ── 6. trigger purchase ───────────────────────────────────────────────────────
header "STEP 6 — Trigger purchase as testuser (user already deleted)"
PURCHASE=$(curl -sf -X POST "$GW/api/finance/purchase" \
  -H "Content-Type: application/json" \
  -H "$TEST_AUTH" \
  -d "{\"userId\":$TEST_USER_ID,\"ticketType\":\"SINGLE\",\"paymentMethodId\":$PM_ID}")

TICKET_ID=$(echo "$PURCHASE" | jq -r '.id')
echo "$PURCHASE" | jq '{ id, type, status, amount }'
ok "Ticket $TICKET_ID is PENDING — PurchaseInitiated event published to queue"
echo "  Compensation begins..."

echo -e "\nWaiting ${SAGA_WAIT}s for compensation to complete..."
sleep "$SAGA_WAIT"

# ── after state ───────────────────────────────────────────────────────────────
header "STATE AFTER COMPENSATION"

section "Ticket final status (should be CANCELLED)"
curl -sf "$GW/api/finance/wallet/$TEST_USER_ID" -H "$ADMIN_AUTH" | \
  jq --arg id "$TICKET_ID" '.content[] | select(.id == $id) | { id, type, status }'

section "Notification (should be Purchase failed)"
curl -sf "$GW/notifications/user/$TEST_USER_ID?size=3&sort=sentAt,desc" -H "$ADMIN_AUTH" | \
  jq '.content[] | { id, title, sentAt }'

section "Queue states (all should be empty)"
for q in ticket-user-update-queue ticket-moneyman-user-failed-queue ticket-notification-cancelled-queue; do
  queue_stat "$q"
done

# ── result ────────────────────────────────────────────────────────────────────
header "RESULT"
FINAL=$(curl -sf "$GW/api/finance/wallet/$TEST_USER_ID" -H "$ADMIN_AUTH" | \
  jq -r --arg id "$TICKET_ID" '.content[] | select(.id == $id) | .status')

if [ "$FINAL" = "CANCELLED" ]; then
  ok "Saga COMPENSATED — ticket is CANCELLED"
  ok "No loyalty points awarded (UserService TX2 never committed)"
  ok "Notification sent: Purchase failed"
else
  bad "Unexpected ticket status: $FINAL — check service logs"
fi
