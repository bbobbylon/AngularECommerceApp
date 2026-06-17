#!/usr/bin/env bash
# =====================================================================
#  One-time Azure setup for the Container Apps deploy (.github/workflows/deploy-azure.yml).
#  Idempotent — safe to re-run. Run in Azure Cloud Shell (bash) or anywhere `az` is logged in
#  (`az login`) against the target subscription.
#
#  Creates: resource group, ACR, an Azure Database for MySQL flexible server + empty database +
#  admin user, a Container Apps environment, and the two container apps (seeded with a placeholder
#  image — the workflow swaps in the real images). Creates a service principal scoped to the resource
#  group for GitHub Actions, and prints the secrets to add. No schema script — Flyway migrates the
#  empty DB on the backend's first boot.
#
#  Override via env vars, e.g.:
#    RESOURCE_GROUP=luv2shop-rg LOCATION=eastus DB_PASS='s3cret!' ./deploy/azure-setup.sh
# =====================================================================
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-luv2shop-rg}"
LOCATION="${LOCATION:-eastus}"
ACR_NAME="${ACR_NAME:-luv2shop$RANDOM}"     # must be globally unique + alphanumeric
ENVIRONMENT="${ENVIRONMENT:-luv2shop-env}"
MYSQL_SERVER="${MYSQL_SERVER:-luv2shop-db-$RANDOM}"   # must be globally unique
DB_NAME="${DB_NAME:-full-stack-ecommerce}"
DB_USER="${DB_USER:-ecommerceapp}"
DB_PASS="${DB_PASS:-}"
PLACEHOLDER="mcr.microsoft.com/k8se/quickstart:latest"

if [[ -z "${DB_PASS}" ]]; then
  # Azure MySQL requires 8-128 chars incl. 3 of {upper,lower,digit,special}.
  DB_PASS="Lv2$(openssl rand -base64 15 | tr -d '/+=' | cut -c1-16)!"
  echo ">> Generated a random DB password (shown at the end)."
fi

SUB_ID="$(az account show --query id -o tsv)"
echo "==> Subscription=${SUB_ID}  RG=${RESOURCE_GROUP}  Location=${LOCATION}  ACR=${ACR_NAME}"

az config set extension.use_dynamic_install=yes_without_prompt >/dev/null 2>&1 || true
az provider register --namespace Microsoft.App --wait >/dev/null 2>&1 || true

echo "==> Resource group…"
az group create -n "${RESOURCE_GROUP}" -l "${LOCATION}" >/dev/null

echo "==> Container Registry '${ACR_NAME}' (admin enabled for turnkey pulls)…"
az acr show -n "${ACR_NAME}" >/dev/null 2>&1 \
  || az acr create -n "${ACR_NAME}" -g "${RESOURCE_GROUP}" --sku Basic --admin-enabled true >/dev/null
ACR_SERVER="$(az acr show -n "${ACR_NAME}" --query loginServer -o tsv)"
ACR_USER="$(az acr credential show -n "${ACR_NAME}" --query username -o tsv)"
ACR_PASS="$(az acr credential show -n "${ACR_NAME}" --query 'passwords[0].value' -o tsv)"

echo "==> MySQL flexible server '${MYSQL_SERVER}' (can take several minutes)…"
az mysql flexible-server show -n "${MYSQL_SERVER}" -g "${RESOURCE_GROUP}" >/dev/null 2>&1 \
  || az mysql flexible-server create -n "${MYSQL_SERVER}" -g "${RESOURCE_GROUP}" -l "${LOCATION}" \
       --admin-user "${DB_USER}" --admin-password "${DB_PASS}" \
       --sku-name Standard_B1ms --tier Burstable --version 8.0.21 \
       --storage-size 20 --public-access 0.0.0.0 --yes >/dev/null
echo "==> Database '${DB_NAME}' (empty — Flyway migrates it on first boot)…"
az mysql flexible-server db show -d "${DB_NAME}" -s "${MYSQL_SERVER}" -g "${RESOURCE_GROUP}" >/dev/null 2>&1 \
  || az mysql flexible-server db create -d "${DB_NAME}" -s "${MYSQL_SERVER}" -g "${RESOURCE_GROUP}" >/dev/null
# Allow Azure services (Container Apps egress) to reach the server.
az mysql flexible-server firewall-rule create -n "${MYSQL_SERVER}" -g "${RESOURCE_GROUP}" \
  --rule-name AllowAzure --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 >/dev/null 2>&1 || true

echo "==> Container Apps environment…"
az containerapp env show -n "${ENVIRONMENT}" -g "${RESOURCE_GROUP}" >/dev/null 2>&1 \
  || az containerapp env create -n "${ENVIRONMENT}" -g "${RESOURCE_GROUP}" -l "${LOCATION}" >/dev/null

create_app () {  # name  target-port
  local NAME="$1" PORT="$2"
  if ! az containerapp show -n "${NAME}" -g "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    az containerapp create -n "${NAME}" -g "${RESOURCE_GROUP}" --environment "${ENVIRONMENT}" \
      --image "${PLACEHOLDER}" --target-port "${PORT}" --ingress external \
      --registry-server "${ACR_SERVER}" --registry-username "${ACR_USER}" --registry-password "${ACR_PASS}" \
      --min-replicas 0 --max-replicas 3 >/dev/null
  fi
}
echo "==> Container apps (placeholder image; the workflow deploys the real ones)…"
create_app luv2shop-backend 8585
create_app luv2shop-frontend 80

echo "==> Service principal for GitHub Actions (Contributor on the resource group)…"
SP_JSON="$(az ad sp create-for-rbac --name "luv2shop-gh-${RESOURCE_GROUP}" \
  --role Contributor \
  --scopes "/subscriptions/${SUB_ID}/resourceGroups/${RESOURCE_GROUP}" \
  --sdk-auth)"

cat <<EOF

=====================================================================
✅ Azure setup complete.

Add these under  GitHub repo → Settings → Secrets and variables → Actions → Secrets:

  AZURE_CREDENTIALS = (the JSON below, verbatim)
  DB_USER           = ${DB_USER}
  DB_PASS           = ${DB_PASS}

  AZURE_CREDENTIALS JSON:
${SP_JSON}

Then edit .github/workflows/deploy-azure.yml 'env:' to:
    ACR_NAME       = ${ACR_NAME}
    RESOURCE_GROUP = ${RESOURCE_GROUP}
    MYSQL_SERVER   = ${MYSQL_SERVER}

Finally: Actions → "Deploy · Azure (Container Apps)" → Run workflow.

NOTE: ACR admin user is enabled for simplicity. For production, disable it and assign the container
apps' managed identity the AcrPull role instead.
=====================================================================
EOF
