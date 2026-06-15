#!/usr/bin/env bash
#
# Luv2Shop - build the backend + frontend, run them, and open the app.
#
#   Run it from Git Bash:   ./run.sh        (or:  bash run.sh)
#   Stop it:                press Ctrl+C in this terminal (stops BOTH servers).
#
#   App (open this):  http://localhost:4250
#   API:              http://localhost:8585/api
#
# Requirements: JDK 21+, Node 20+, and Docker running (the backend auto-starts MySQL on :3307).
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_URL="http://localhost:4250"
API_URL="http://localhost:8585/api"
BACKEND_PID=""

# Ctrl+C (or exit) stops the backend too, so one terminal controls the whole app.
cleanup() {
  trap - INT TERM EXIT
  echo ""
  echo "Stopping Luv2Shop (backend + frontend)..."
  [ -n "${BACKEND_PID:-}" ] && kill "$BACKEND_PID" >/dev/null 2>&1 || true
  for PORT in 8585 4250; do
    PID="$(netstat -ano 2>/dev/null | grep ":$PORT" | grep -i LISTENING | awk '{print $NF}' | head -1)"
    [ -n "${PID:-}" ] && MSYS_NO_PATHCONV=1 taskkill /F /PID "$PID" /T >/dev/null 2>&1 || true
  done
  echo "Stopped. (MySQL is still running -> 'cd backend && docker compose down' to stop it.)"
}
trap cleanup INT TERM EXIT

echo "=== Building backend (Maven) ==="
( cd "$ROOT/backend" && ./mvnw -q -DskipTests clean package )
echo "Backend build OK."

echo ""
echo "=== Preparing frontend (Angular) ==="
cd "$ROOT/frontend/angular-ecommerce"
if [ ! -d node_modules ]; then
  echo "Installing npm dependencies (first run)..."
  npm install
fi

echo ""
echo "=== Starting backend in the background (logs -> backend.log) ==="
( cd "$ROOT/backend" && ./mvnw spring-boot:run > "$ROOT/backend.log" 2>&1 ) &
BACKEND_PID=$!

# Wait for the backend to actually serve data BEFORE opening the browser, so the first
# page load isn't an empty "no products found" (the API call would otherwise beat the backend).
echo ""
echo "Waiting for the backend API on :8585 (first run ~40s while MySQL starts)..."
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

Starting the frontend - the app opens in your browser automatically when ready (~10-20s).
Make sure Docker is running so the backend can start MySQL (:3307).

  Backend logs:     tail -f backend.log
  Stop everything:  press Ctrl+C here.

EOF

# Foreground: compiles + serves the frontend on :4250 and opens the browser.
# Pressing Ctrl+C here returns control to the trap above, which stops the backend too.
npx ng serve --port 4250 --open
