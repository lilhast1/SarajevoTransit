#!/usr/bin/env bash
# Saga verification script — purchases a ticket and shows state before/after.
# Usage: ./saga-verify.sh [TICKET_TYPE]
#   TICKET_TYPE: SINGLE (default) | DAILY | WEEKLY | MONTHLY

set -euo pipefail

GW="http://localhost:8080"
EMAIL="amina.hadzic@sarajevotransit.ba"
PASSWORD="AminaPass123"
USER_ID=2
PAYMENT_METHOD_ID=1
TICKET_TYPE="${1:-SINGLE}"
SAGA_WAIT=3   # seconds to wait for async saga to complete

# ── colours ──────────────────────────────────────────────────────────────────
BOLD='\033[1m'; CYAN='\033[0;36m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; RESET='\033[0m'

header()  { echo -e "\n${BOLD}${CYAN}══ $* ══${RESET}"; }
section() { echo -e "\n${BOLD}${YELLOW}── $* ──${RESET}"; }
ok()      { echo -e "${GREEN}[OK] $*${RESET}"; }

# ── authenticate ─────────────────────────────────────────────────────────────
header "AUTHENTICATING"
echo "User : $EMAIL"

AUTH=$(curl -sf -X POST "$GW/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo "$AUTH" | jq -r '.accessToken')
EXPIRES_IN=$(echo "$AUTH" | jq -r '.expiresIn')
ok "Token obtained (expires in ${EXPIRES_IN}s)"

AUTH_HEADER="Authorization: Bearer $TOKEN"

# ── helper: snapshot ─────────────────────────────────────────────────────────
snapshot() {
  local label="$1"
  header "$label"

  section "Loyalty balance"
  curl -sf "$GW/api/users/$USER_ID/loyalty/balance" -H "$AUTH_HEADER" | \
    jq '{ userId, currentBalance }'

  section "Loyalty transactions (last 5)"
  curl -sf "$GW/api/users/$USER_ID/loyalty/transactions?page=0&size=5&sort=createdAt,desc" \
    -H "$AUTH_HEADER" | \
    jq '.content[] | { type, points, description, createdAt }' 2>/dev/null || echo "(none)"

  section "Wallet tickets (last 5)"
  curl -sf "$GW/api/finance/wallet/$USER_ID?size=5&sort=purchaseDate,desc" \
    -H "$AUTH_HEADER" | \
    jq '.content[] | { id, type, status, purchaseDate, amount }'

  section "Notifications (last 5)"
  curl -sf "$GW/notifications/user/$USER_ID?size=5&sort=sentAt,desc" \
    -H "$AUTH_HEADER" | \
    jq '.content[] | { id, title, type, sentAt }' 2>/dev/null || echo "(none)"
}

# ── before ───────────────────────────────────────────────────────────────────
snapshot "STATE BEFORE PURCHASE"

# ── trigger saga ─────────────────────────────────────────────────────────────
header "TRIGGERING SAGA  —  ticket type: $TICKET_TYPE"

PURCHASE=$(curl -sf -X POST "$GW/api/finance/purchase" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d "{\"userId\":$USER_ID,\"ticketType\":\"$TICKET_TYPE\",\"paymentMethodId\":$PAYMENT_METHOD_ID}")

TICKET_ID=$(echo "$PURCHASE" | jq -r '.id')
INITIAL_STATUS=$(echo "$PURCHASE" | jq -r '.status')

echo "$PURCHASE" | jq '{ id, type, status, amount, externalTransactionId }'
ok "Purchase accepted — ticket $TICKET_ID is $INITIAL_STATUS"

# ── wait for saga ─────────────────────────────────────────────────────────────
echo -e "\nWaiting ${SAGA_WAIT}s for async saga to complete..."
sleep "$SAGA_WAIT"

# ── poll final ticket status ──────────────────────────────────────────────────
section "Final ticket status"
curl -sf "$GW/api/finance/wallet/$USER_ID?size=20" -H "$AUTH_HEADER" | \
  jq --arg id "$TICKET_ID" '.content[] | select(.id == $id) | { id, type, status, validUntil }'

# ── after ─────────────────────────────────────────────────────────────────────
snapshot "STATE AFTER PURCHASE"

# ── diff summary ──────────────────────────────────────────────────────────────
header "SAGA RESULT"
FINAL_STATUS=$(curl -sf "$GW/api/finance/wallet/$USER_ID?size=20" -H "$AUTH_HEADER" | \
  jq -r --arg id "$TICKET_ID" '.content[] | select(.id == $id) | .status')

if [ "$FINAL_STATUS" = "ACTIVE" ]; then
  ok "Saga COMPLETED — ticket flipped $INITIAL_STATUS → $FINAL_STATUS"
  echo "   Loyalty points were awarded based on ticket type:"
  echo "     SINGLE=2  DAILY=5  WEEKLY=20  MONTHLY=50"
else
  echo -e "\n[WARN] Ticket status is '$FINAL_STATUS' — saga may have failed or is still running."
  echo "  Check service logs for Saga [...] entries."
fi
