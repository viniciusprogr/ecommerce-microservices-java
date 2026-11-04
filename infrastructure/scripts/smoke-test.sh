#!/usr/bin/env bash
# Smoke test ponta a ponta contra o ambiente do docker compose (tudo pelo API Gateway).
# Requisitos: curl e jq.  Uso:  ./infrastructure/scripts/smoke-test.sh
# Le as credenciais do .env (ou das variaveis de ambiente ja definidas).
set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:8080}"
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
ADMIN_USER="${GATEWAY_ADMIN_USERNAME:-admin}"
NORMAL_USER="${GATEWAY_USER_USERNAME:-user}"
: "${GATEWAY_ADMIN_PASSWORD:?GATEWAY_ADMIN_PASSWORD is required}"
: "${GATEWAY_USER_PASSWORD:?GATEWAY_USER_PASSWORD is required}"

fail() { echo "FAIL: $*" >&2; exit 1; }
step() { echo "==> $*"; }

# request METHOD PATH TOKEN [BODY] -> imprime "<status> <body>"
request() {
  local method=$1 path=$2 token=$3 body=${4:-}
  local args=(-s -o /tmp/smoke-body -w '%{http_code}' -X "$method" "$GATEWAY$path" -H 'Content-Type: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$body" ]] && args+=(-d "$body")
  local status
  status=$(curl "${args[@]}")
  echo "$status $(cat /tmp/smoke-body)"
}

expect() {
  local expected=$1 response=$2 label=$3
  local status=${response%% *}
  [[ "$status" == "$expected" ]] || fail "$label: expected HTTP $expected, got $response"
  echo "    ok ($status) $label"
}

body() { echo "${1#* }"; }

login() {
  local response
  response=$(request POST /auth/login "" "{\"username\":\"$1\",\"password\":\"$2\"}")
  expect 200 "$response" "login $1" >&2
  body "$response" | jq -r '.accessToken'
}

SUFFIX=$(date +%s)

step "Login"
ADMIN_TOKEN=$(login "$ADMIN_USER" "$GATEWAY_ADMIN_PASSWORD")
USER_TOKEN=$(login "$NORMAL_USER" "$GATEWAY_USER_PASSWORD")
expect 401 "$(request GET /api/products "")" "request without token is rejected"

step "Catalog (admin)"
R=$(request POST /api/categories "$ADMIN_TOKEN" "{\"name\":\"Smoke $SUFFIX\"}"); expect 201 "$R" "create category"
CATEGORY_ID=$(body "$R" | jq -r '.id')
R=$(request POST /api/products "$ADMIN_TOKEN" \
  "{\"sku\":\"SMOKE-$SUFFIX\",\"name\":\"Smoke Mouse\",\"price\":49.90,\"categoryId\":$CATEGORY_ID}")
expect 201 "$R" "create product"
PRODUCT_ID=$(body "$R" | jq -r '.id')
expect 403 "$(request POST /api/categories "$USER_TOKEN" '{"name":"forbidden"}')" "user cannot write catalog"

step "Inventory (admin)"
expect 201 "$(request POST /api/inventory "$ADMIN_TOKEN" "{\"productId\":$PRODUCT_ID,\"quantity\":5}")" "create stock"

step "Customer"
CPF_LIST=(52998224725 11144477735 39053344705 86288366757 71428793860)
CPF=${CPF_LIST[$((SUFFIX % ${#CPF_LIST[@]}))]}
R=$(request POST /api/customers "$USER_TOKEN" \
  "{\"firstName\":\"Smoke\",\"lastName\":\"Test\",\"email\":\"smoke$SUFFIX@example.com\",\"document\":\"$CPF\"}")
if [[ "${R%% *}" == "409" ]]; then
  CUSTOMER_ID=$(body "$(request GET "/api/customers?size=100" "$ADMIN_TOKEN")" | jq -r ".content[] | select(.document==\"$CPF\") | .id")
  echo "    ok (409) customer with this CPF already exists, reusing id $CUSTOMER_ID"
else
  expect 201 "$R" "create customer"
  CUSTOMER_ID=$(body "$R" | jq -r '.id')
fi

step "Order flow"
R=$(request POST /api/orders "$USER_TOKEN" "{\"customerId\":$CUSTOMER_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":2}]}")
expect 201 "$R" "create order"
ORDER_ID=$(body "$R" | jq -r '.id')
[[ "$(body "$R" | jq -r '.totalAmount')" == "99.8" ]] || fail "unexpected total: $(body "$R")"
[[ "$(body "$(request GET "/api/inventory/$PRODUCT_ID" "$USER_TOKEN")" | jq -r '.available')" == "3" ]] || fail "stock not reserved"
echo "    ok stock reserved (available=3)"

expect 409 "$(request POST /api/orders "$USER_TOKEN" "{\"customerId\":$CUSTOMER_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":4}]}")" \
  "order rejected when stock is insufficient"
expect 403 "$(request PATCH "/api/orders/$ORDER_ID/status" "$USER_TOKEN" '{"status":"SHIPPED"}')" "user cannot change status"
expect 200 "$(request PATCH "/api/orders/$ORDER_ID/status" "$ADMIN_TOKEN" '{"status":"SHIPPED"}')" "admin ships order"
[[ "$(body "$(request GET "/api/inventory/$PRODUCT_ID" "$USER_TOKEN")" | jq -r '.quantityOnHand')" == "3" ]] || fail "stock not committed"
echo "    ok stock committed (onHand=3)"
expect 409 "$(request PATCH "/api/orders/$ORDER_ID/status" "$ADMIN_TOKEN" '{"status":"CANCELLED"}')" "shipped order cannot be cancelled"
expect 403 "$(request POST /api/inventory/reservations "$ADMIN_TOKEN" '{}')" "internal reservation API blocked at the gateway"

echo "SMOKE TEST PASSED"
