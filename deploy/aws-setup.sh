#!/usr/bin/env bash
# =====================================================================
#  One-time AWS setup for the App Runner deploy (.github/workflows/deploy-aws.yml).
#  Idempotent — safe to re-run. Run in AWS CloudShell or anywhere the AWS CLI is configured
#  (`aws configure`) for the target account/region.
#
#  CONFIG lives in ONE place: deploy/aws.env (this script reads it; so does the workflow).
#  Edit AWS_REGION there, then just run this script — no flags needed. Any value can still be
#  overridden inline, e.g.:  AWS_REGION=us-west-2 DB_PASS='s3cret!' ./deploy/aws-setup.sh
#
#  Creates: two ECR repos, the App Runner ECR-access role, an RDS MySQL instance (publicly
#  accessible, in a security group that allows 3306) + an empty database, and a deployer IAM user
#  with access keys. The App Runner SERVICES themselves are created by the workflow on its first run
#  (App Runner can't create an ECR service before the image exists). If the `gh` CLI is authenticated
#  it pushes the three GitHub secrets for you; otherwise it prints them. No schema script — Flyway
#  migrates the empty DB on the backend's first boot.
#
#  ⚠️  SECURITY: for turnkey demo connectivity this opens RDS 3306 to 0.0.0.0/0. For production, put
#      RDS in a private subnet and reach it via an App Runner VPC connector instead (see DEPLOYMENT.md).
# =====================================================================
set -euo pipefail

# --- Load deploy/aws.env as defaults (inline env vars still win) -------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/aws.env}"
if [[ -f "${ENV_FILE}" ]]; then
  echo "==> Loading config from ${ENV_FILE}"
  while IFS='=' read -r key val; do
    [[ "${key}" =~ ^[A-Z_]+$ ]] || continue
    val="${val%$'\r'}"
    [[ -z "${!key:-}" ]] && export "${key}=${val}"
  done < "${ENV_FILE}"
fi

AWS_REGION="${AWS_REGION:-us-east-1}"
DB_INSTANCE="${DB_INSTANCE:-luv2shop-db}"
DB_NAME="${DB_NAME:-fullstackecommerce}"     # RDS initial DB name — no hyphens allowed
DB_USER="${DB_USER:-ecommerceapp}"
DB_PASS="${DB_PASS:-}"
BACKEND_SERVICE="${BACKEND_SERVICE:-luv2shop-backend}"
FRONTEND_SERVICE="${FRONTEND_SERVICE:-luv2shop-frontend}"
DEPLOYER_USER="${DEPLOYER_USER:-luv2shop-gh-deployer}"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
export AWS_PAGER=""

if [[ -z "${DB_PASS}" ]]; then
  DB_PASS="$(openssl rand -base64 18 | tr -d '/+=@"' | cut -c1-20)"
  echo ">> Generated a random DB password (handled below)."
fi
echo "==> Account=${ACCOUNT_ID}  Region=${AWS_REGION}"

echo "==> ECR repositories…"
for REPO in "${BACKEND_SERVICE}" "${FRONTEND_SERVICE}"; do
  aws ecr describe-repositories --repository-names "$REPO" --region "$AWS_REGION" >/dev/null 2>&1 \
    || aws ecr create-repository --repository-name "$REPO" --region "$AWS_REGION" >/dev/null
done

echo "==> App Runner ECR-access role…"
if ! aws iam get-role --role-name AppRunnerECRAccessRole >/dev/null 2>&1; then
  aws iam create-role --role-name AppRunnerECRAccessRole \
    --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"build.apprunner.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
fi
aws iam attach-role-policy --role-name AppRunnerECRAccessRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess >/dev/null 2>&1 || true

echo "==> Security group for RDS (allows 3306)…"
VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text --region "$AWS_REGION")"
SG_ID="$(aws ec2 describe-security-groups --filters Name=group-name,Values=luv2shop-rds-sg Name=vpc-id,Values=$VPC_ID \
  --query 'SecurityGroups[0].GroupId' --output text --region "$AWS_REGION" 2>/dev/null)"
if [[ "$SG_ID" == "None" || -z "$SG_ID" ]]; then
  SG_ID="$(aws ec2 create-security-group --group-name luv2shop-rds-sg --description 'Luv2Shop RDS access' \
    --vpc-id "$VPC_ID" --query GroupId --output text --region "$AWS_REGION")"
fi
aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 3306 \
  --cidr 0.0.0.0/0 --region "$AWS_REGION" >/dev/null 2>&1 || true

echo "==> RDS MySQL instance '${DB_INSTANCE}' (this takes several minutes the first time)…"
if ! aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE" --region "$AWS_REGION" >/dev/null 2>&1; then
  aws rds create-db-instance --db-instance-identifier "$DB_INSTANCE" \
    --db-instance-class db.t3.micro --engine mysql --engine-version 8.0 \
    --master-username "$DB_USER" --master-user-password "$DB_PASS" \
    --allocated-storage 20 --db-name "$DB_NAME" --publicly-accessible \
    --vpc-security-group-ids "$SG_ID" --backup-retention-period 1 --no-multi-az \
    --region "$AWS_REGION" >/dev/null
fi
echo "    waiting for the instance to become available…"
aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE" --region "$AWS_REGION"
RDS_ENDPOINT="$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE" \
  --query 'DBInstances[0].Endpoint.Address' --output text --region "$AWS_REGION")"

echo "==> Deployer IAM user '${DEPLOYER_USER}'…"
aws iam get-user --user-name "$DEPLOYER_USER" >/dev/null 2>&1 || aws iam create-user --user-name "$DEPLOYER_USER" >/dev/null
aws iam attach-user-policy --user-name "$DEPLOYER_USER" --policy-arn arn:aws:iam::aws:policy/AWSAppRunnerFullAccess >/dev/null 2>&1 || true
aws iam attach-user-policy --user-name "$DEPLOYER_USER" --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser >/dev/null 2>&1 || true
# App Runner needs to PassRole the ECR-access role and create its service-linked role on first use.
aws iam put-user-policy --user-name "$DEPLOYER_USER" --policy-name luv2shop-passrole \
  --policy-document "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"iam:PassRole\",\"iam:CreateServiceLinkedRole\"],\"Resource\":\"*\"}]}" >/dev/null
KEY_JSON="$(aws iam create-access-key --user-name "$DEPLOYER_USER" --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)"
AK="$(echo "$KEY_JSON" | awk '{print $1}')"; SK="$(echo "$KEY_JSON" | awk '{print $2}')"

# --- Wire up the GitHub secrets automatically if `gh` is available -----------
GH_ARGS=(); [[ -n "${GH_REPO:-}" ]] && GH_ARGS=(-R "${GH_REPO}")
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  echo "==> gh CLI detected — pushing GitHub Actions secrets (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, DB_PASS)…"
  printf '%s' "$AK"      | gh secret set AWS_ACCESS_KEY_ID     "${GH_ARGS[@]}"
  printf '%s' "$SK"      | gh secret set AWS_SECRET_ACCESS_KEY "${GH_ARGS[@]}"
  printf '%s' "$DB_PASS" | gh secret set DB_PASS               "${GH_ARGS[@]}"
  SECRETS_DONE=1
else
  SECRETS_DONE=0
fi

echo ""
echo "====================================================================="
echo "✅ AWS setup complete.  (RDS endpoint: ${RDS_ENDPOINT} — the workflow discovers this automatically.)"
echo ""
if [[ "${SECRETS_DONE}" == "1" ]]; then
  cat <<EOF
✅ GitHub secrets pushed (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, DB_PASS).

Nothing else to configure — deploy/aws.env is the single source of truth the workflow reads, and it
discovers your account id + RDS endpoint at runtime. Just run:

    Actions → "Deploy · AWS (App Runner)" → Run workflow.

The first run CREATES the App Runner services (subsequent runs update them).
EOF
else
  cat <<EOF
The 'gh' CLI isn't authenticated here, so add these three secrets by hand under
GitHub repo → Settings → Secrets and variables → Actions → Secrets:

  AWS_ACCESS_KEY_ID     = ${AK}
  AWS_SECRET_ACCESS_KEY = ${SK}
  DB_PASS               = ${DB_PASS}

(Or set GH_REPO=owner/repo and re-run where 'gh auth login' is done to push them automatically.)

Everything else (region, DB name/user, service names) already lives in deploy/aws.env, and the
workflow discovers your account id + RDS endpoint at runtime — no YAML edits, no repo variables.

Then: Actions → "Deploy · AWS (App Runner)" → Run workflow.
EOF
fi
echo ""
echo "⚠️  The access key above is a credential — keep it only in the GitHub secret."
echo "⚠️  RDS is open on 3306 to the internet for turnkey access; lock it down (VPC connector) for production."
echo "====================================================================="
