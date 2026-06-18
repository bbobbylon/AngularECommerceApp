#!/usr/bin/env bash
# =====================================================================
#  One-time GCP setup for the Cloud Run deploy (.github/workflows/deploy-gcp.yml).
#  Idempotent — safe to re-run. Run it in Google Cloud Shell or anywhere `gcloud` is
#  authenticated (`gcloud auth login`) against the target project.
#
#  CONFIG lives in ONE place: deploy/gcp.env (this script reads it; so does the
#  workflow). Edit GCP_PROJECT there, then just run this script — no flags needed.
#  Any value can still be overridden inline, e.g.:
#    GCP_PROJECT=my-proj GCP_REGION=europe-west1 DB_PASS='s3cret!' ./deploy/gcp-setup.sh
#
#  It creates: the Artifact Registry repo, a Cloud SQL (MySQL 8) instance + database + user,
#  a GitHub-Actions "deployer" service account with the right roles, and grants the Cloud Run
#  RUNTIME service account `roles/cloudsql.client` (so the app's Cloud SQL connector can dial
#  the instance). It then writes the deployer key to ./key.json and — if the `gh` CLI is
#  authenticated — pushes the two GitHub secrets (GCP_SA_KEY, DB_PASS) for you and deletes the
#  key. Otherwise it prints them to paste in by hand. No schema script is run — Flyway migrates
#  the empty DB on the backend's first boot.
# =====================================================================
set -euo pipefail

# --- Load deploy/gcp.env as defaults (inline env vars still win) -------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/gcp.env}"
if [[ -f "${ENV_FILE}" ]]; then
  echo "==> Loading config from ${ENV_FILE}"
  while IFS='=' read -r key val; do
    [[ "${key}" =~ ^[A-Z_]+$ ]] || continue       # skip comments / blank lines
    val="${val%$'\r'}"                              # tolerate CRLF line endings
    [[ -z "${!key:-}" ]] && export "${key}=${val}"  # only if not already set inline
  done < "${ENV_FILE}"
fi

GCP_PROJECT="${GCP_PROJECT:-$(gcloud config get-value project 2>/dev/null)}"
GCP_REGION="${GCP_REGION:-us-central1}"
AR_REPO="${AR_REPO:-luv2shop}"
CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE:-luv2shop-db}"
CLOUD_SQL_TIER="${CLOUD_SQL_TIER:-db-f1-micro}"
DB_NAME="${DB_NAME:-full-stack-ecommerce}"
DB_USER="${DB_USER:-ecommerceapp}"
DB_PASS="${DB_PASS:-}"          # if empty, a random password is generated and printed
DEPLOYER_SA="${DEPLOYER_SA:-gh-deployer}"

if [[ -z "${GCP_PROJECT}" || "${GCP_PROJECT}" == "(unset)" || "${GCP_PROJECT}" == "your-project-id" ]]; then
  echo "ERROR: set GCP_PROJECT in deploy/gcp.env (or 'gcloud config set project <id>') first." >&2
  exit 1
fi
if [[ -z "${DB_PASS}" ]]; then
  DB_PASS="$(openssl rand -base64 18 | tr -d '/+=' | cut -c1-20)"
  echo ">> Generated a random DB password (handled below)."
fi

echo "==> Project=${GCP_PROJECT}  Region=${GCP_REGION}  Instance=${CLOUD_SQL_INSTANCE}"
gcloud config set project "${GCP_PROJECT}" >/dev/null

echo "==> Enabling required APIs (idempotent)…"
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  sqladmin.googleapis.com \
  iamcredentials.googleapis.com

echo "==> Artifact Registry repo '${AR_REPO}'…"
gcloud artifacts repositories describe "${AR_REPO}" --location="${GCP_REGION}" >/dev/null 2>&1 \
  || gcloud artifacts repositories create "${AR_REPO}" \
       --repository-format=docker --location="${GCP_REGION}" \
       --description="Luv2Shop container images"

echo "==> Cloud SQL instance '${CLOUD_SQL_INSTANCE}' (this can take a few minutes the first time)…"
gcloud sql instances describe "${CLOUD_SQL_INSTANCE}" >/dev/null 2>&1 \
  || gcloud sql instances create "${CLOUD_SQL_INSTANCE}" \
       --database-version=MYSQL_8_0 --tier="${CLOUD_SQL_TIER}" --region="${GCP_REGION}"

echo "==> Database '${DB_NAME}' (empty — Flyway migrates it on first boot)…"
gcloud sql databases describe "${DB_NAME}" --instance="${CLOUD_SQL_INSTANCE}" >/dev/null 2>&1 \
  || gcloud sql databases create "${DB_NAME}" --instance="${CLOUD_SQL_INSTANCE}"

echo "==> Database user '${DB_USER}'…"
if gcloud sql users list --instance="${CLOUD_SQL_INSTANCE}" --format='value(name)' | grep -qx "${DB_USER}"; then
  gcloud sql users set-password "${DB_USER}" --instance="${CLOUD_SQL_INSTANCE}" --password="${DB_PASS}"
else
  gcloud sql users create "${DB_USER}" --instance="${CLOUD_SQL_INSTANCE}" --password="${DB_PASS}"
fi

SA_EMAIL="${DEPLOYER_SA}@${GCP_PROJECT}.iam.gserviceaccount.com"
echo "==> Deployer service account '${SA_EMAIL}'…"
gcloud iam service-accounts describe "${SA_EMAIL}" >/dev/null 2>&1 \
  || gcloud iam service-accounts create "${DEPLOYER_SA}" --display-name="GitHub Actions deployer"

echo "==> Granting deployer roles (run.admin, artifactregistry.writer, iam.serviceAccountUser)…"
for ROLE in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser; do
  gcloud projects add-iam-policy-binding "${GCP_PROJECT}" \
    --member="serviceAccount:${SA_EMAIL}" --role="${ROLE}" --condition=None >/dev/null
done

# The Cloud Run service runs as the default compute service account by default; the Cloud SQL
# JDBC connector authenticates as THAT identity, so it needs roles/cloudsql.client.
PROJECT_NUMBER="$(gcloud projects describe "${GCP_PROJECT}" --format='value(projectNumber)')"
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
echo "==> Granting the Cloud Run runtime SA (${RUNTIME_SA}) roles/cloudsql.client…"
gcloud projects add-iam-policy-binding "${GCP_PROJECT}" \
  --member="serviceAccount:${RUNTIME_SA}" --role="roles/cloudsql.client" --condition=None >/dev/null

echo "==> Creating deployer key -> ./key.json…"
gcloud iam service-accounts keys create key.json --iam-account="${SA_EMAIL}"

# --- Wire up the two GitHub secrets automatically if `gh` is available --------
GH_ARGS=()
[[ -n "${GH_REPO:-}" ]] && GH_ARGS=(-R "${GH_REPO}")
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  echo "==> gh CLI detected — pushing GitHub Actions secrets (GCP_SA_KEY, DB_PASS)…"
  gh secret set GCP_SA_KEY "${GH_ARGS[@]}" < key.json
  printf '%s' "${DB_PASS}" | gh secret set DB_PASS "${GH_ARGS[@]}"
  rm -f key.json
  SECRETS_DONE=1
else
  SECRETS_DONE=0
fi

cat <<EOF

=====================================================================
✅ GCP setup complete.

EOF

if [[ "${SECRETS_DONE}" == "1" ]]; then
  cat <<EOF
✅ GitHub secrets pushed for you (GCP_SA_KEY, DB_PASS); ./key.json deleted.

Nothing else to configure — deploy/gcp.env is already the single source of truth
the workflow reads. Just run:

    Actions → "Deploy · GCP (Cloud Run)" → Run workflow.

One run deploys both tiers.
=====================================================================
EOF
else
  cat <<EOF
The 'gh' CLI isn't authenticated here, so add these two secrets by hand under
GitHub repo → Settings → Secrets and variables → Actions → Secrets:

  GCP_SA_KEY = (paste the full contents of ./key.json)
  DB_PASS    = ${DB_PASS}

(Or run this script where 'gh auth login' has been done, or set GH_REPO=owner/repo
and re-run, to have them pushed automatically.)

Everything else (project, region, repo, instance, DB name/user) already lives in
deploy/gcp.env, which the workflow reads — no YAML editing needed.

Then: Actions → "Deploy · GCP (Cloud Run)" → Run workflow. One run deploys both tiers.

⚠️  ./key.json is a credential — do NOT commit it. Delete it after pasting:  rm key.json
=====================================================================
EOF
fi
