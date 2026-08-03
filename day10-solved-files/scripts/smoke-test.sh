#!/usr/bin/env bash
# =============================================================================
# TICKET-ADV153 — Smoke-test script
# Brings up the full stack, waits for every service to be healthy, exercises
# a handful of key endpoints, then tears down.
# Usage:  bash scripts/smoke-test.sh
# =============================================================================
set -euo pipefail

BACKEND="http://localhost:8081/api"
TIMEOUT=120   # seconds to wait for health
INTERVAL=5

log()  { echo "[smoke] $*"; }
fail() { echo "[smoke] FAIL: $*" >&2; docker compose down -v 2>/dev/null; exit 1; }

# ── 1. Bring up the stack ────────────────────────────────────────────────────
log "Starting stack..."
docker compose up -d --build

# ── 2. Wait for backend health ───────────────────────────────────────────────
log "Waiting for backend health (timeout ${TIMEOUT}s)..."
elapsed=0
until curl -sf "${BACKEND}/actuator/health" | grep -q '"status":"UP"'; do
  sleep $INTERVAL
  elapsed=$((elapsed + INTERVAL))
  [ $elapsed -ge $TIMEOUT ] && fail "backend did not become healthy in ${TIMEOUT}s"
  log "  ...still waiting (${elapsed}s)"
done
log "Backend is UP."

# ── 3. Auth — obtain a JWT ───────────────────────────────────────────────────
log "Authenticating..."
TOKEN=$(curl -sf -X POST "${BACKEND}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
[ -z "$TOKEN" ] && fail "could not obtain JWT"
AUTH="Authorization: Bearer $TOKEN"
log "JWT obtained."

# ── 4. POST a trade ──────────────────────────────────────────────────────────
log "Creating a trade..."
TRADE_ID=$(curl -sf -X POST "${BACKEND}/trades" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{
    "tradeRef":"SMOKE-001",
    "instrument":"AAPL",
    "direction":"BUY",
    "quantity":100,
    "price":150.00,
    "tradeDate":"2025-01-01",
    "settlementDate":"2025-01-03",
    "counterparty":"CPTY-A",
    "currency":"USD"
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
[ -z "$TRADE_ID" ] && fail "could not create trade"
log "Trade created: id=$TRADE_ID"

# ── 5. GET the trade back ────────────────────────────────────────────────────
log "Fetching trade $TRADE_ID..."
curl -sf -H "$AUTH" "${BACKEND}/trades/$TRADE_ID" | python3 -c "
import sys, json
t = json.load(sys.stdin)
assert t['tradeRef'] == 'SMOKE-001', 'tradeRef mismatch'
print('  tradeRef OK:', t['tradeRef'])
"

# ── 6. Actuator metrics reachable ────────────────────────────────────────────
log "Checking metrics endpoint..."
curl -sf "${BACKEND}/actuator/prometheus" | grep -q "jvm_memory" || fail "metrics missing"
log "Prometheus metrics OK."

# ── 7. Prometheus scrape target up ───────────────────────────────────────────
log "Checking Prometheus targets..."
curl -sf "http://localhost:9090/api/v1/targets" | \
  python3 -c "
import sys,json
d=json.load(sys.stdin)
ups=[t for t in d['data']['activeTargets'] if t['health']=='up']
print(f'  {len(ups)} target(s) UP')
assert len(ups) >= 1, 'no Prometheus targets up'
"

# ── 8. Tear down ─────────────────────────────────────────────────────────────
log "Tearing down..."
docker compose down -v
log "All smoke tests PASSED."
