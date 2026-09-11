#!/usr/bin/env bash
#
# Luv2Shop - build + run a full-stack, containerized deployment ON DIFFERENT PORTS than the normal
# dev setup (./run.sh or `docker compose up`), so it can run side-by-side with an already-running
# instance without a port clash. Builds fresh backend + frontend images every run.
#
#   Run it from Git Bash:   ./deploy.sh        (or:  bash deploy.sh)
#   Stop it:                ./deploy.sh down    (or: docker compose -f compose.deploy.yaml -p luv2shop-deploy down)
#
#   App (open this):  http://localhost:4251
#   API:              http://localhost:8586/api
#   MySQL:            localhost:3308
#
# Requirements: Docker running. This mirrors compose.yaml (prod-profile containers, Flyway-managed
# schema) — see docs/DEPLOYMENT.md.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT/compose.deploy.yaml"
PROJECT="luv2shop-deploy"
APP_URL="http://localhost:4251"
API_URL="http://localhost:8586/api"

if [ "${1:-}" = "down" ]; then
  echo "Stopping the alt-port deployment..."
  docker compose -f "$COMPOSE_FILE" -p "$PROJECT" down
  exit 0
fi

echo "=== Building + starting Luv2Shop (alt ports: frontend 4251, backend 8586, MySQL 3308) ==="
docker compose -f "$COMPOSE_FILE" -p "$PROJECT" up --build -d

echo ""
echo "Waiting for the backend API on :8586 (first run ~40s while MySQL starts + Flyway migrates)..."
for i in $(seq 1 120); do
  if curl -sf -o /dev/null "$API_URL/products" 2>/dev/null; then
    echo "Backend is UP and serving data."
    break
  fi
  sleep 2
done

cat <<EOF

  ===================================================
    OPEN THE APP:  $APP_URL
    Backend API :  $API_URL/products
  ===================================================

  Logs:             docker compose -f compose.deploy.yaml -p $PROJECT logs -f
  Stop everything:  ./deploy.sh down

EOF

if command -v explorer.exe >/dev/null 2>&1; then
  explorer.exe "$APP_URL" >/dev/null 2>&1 || true
elif command -v open >/dev/null 2>&1; then
  open "$APP_URL" || true
fi
