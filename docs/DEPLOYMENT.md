# Luv2Shop — CI/CD & Deployment

> **Local development stays primary.** `./run.sh` is how you build and run day to day.
> Everything below is **opt-in**: the full-stack container deploy is one command, CI runs build+test
> on push, and the cloud deploys are **manual templates** (`workflow_dispatch`) you fill in with your
> own project IDs, registries, and secrets. Nothing here changes your local dev workflow.

The path this guide walks: **run it locally in containers → prove it works → push the same images to a
cloud.** A cloud deploy is the local container deploy with the registry and database swapped for managed
services.

- [Deploy locally (full stack in containers)](#deploy-locally-full-stack-in-containers) — start here
- [Continuous Integration](#continuous-integration)
- [Container images](#container-images)
- [From local to the cloud](#from-local-to-the-cloud)
- [Cloud deployments](#cloud-deployments) — [GCP](#google-cloud-cloud-run) · [AWS](#aws-app-runner) · [Azure](#azure-container-apps)
- [Walkthrough: GCP Cloud Run, end to end](#walkthrough-gcp-cloud-run-end-to-end)
- [What you configure](#what-you-configure)

---

## Deploy locally (full stack in containers)

The repo-root [`compose.yaml`](../compose.yaml) builds and runs **all three tiers** — the Angular SPA
(nginx), the Spring Boot API, and MySQL 8 — as containers, using the very same images the cloud runs.
This is the production-shaped deploy you can run on your laptop.

```bash
# from the repo root
docker compose up --build          # add -d to run detached
```

Then open **http://localhost:4250**. The API is on **http://localhost:8585**, MySQL on **3307**.

What happens on first boot, with no manual DB setup:
1. MySQL starts; Compose waits for its healthcheck before starting the backend.
2. The backend boots under the **`prod` profile** and **Flyway migrates the empty database**
   (`V1` baseline → `V2` indexes); `ddl-auto=validate` then confirms the entities match.
3. The `DataLoader` seeds the catalog, so the store is populated the moment the page loads.

```bash
docker compose ps                         # see container/health status
docker compose logs -f backend            # follow backend logs (ECS JSON in prod profile)
curl http://localhost:8585/actuator/health/readiness   # -> {"status":"UP"}
docker compose down                       # stop; add -v to also drop the MySQL volume
```

Optional integrations (Stripe, Okta, email) are off by default and degrade gracefully — uncomment the
env vars in `compose.yaml` to enable them. This is also the fastest way to **smoke-test a release
candidate** the way it will run in the cloud.

---

## Continuous Integration

`.github/workflows/ci.yml` runs on every push / PR — pure verification, no deploys.

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8','clusterBkg':'#f5f7fd','clusterBorder':'#e7ecf7'}}}%%
flowchart LR
  dev([👩‍💻 Developer]):::user -->|"git push / PR"| gh["GitHub repo"]:::ext
  gh --> ci{{"GitHub Actions · CI"}}:::accent
  subgraph jobs["parallel jobs"]
    be["Backend<br/>setup-java 21 · ./mvnw clean package<br/>JUnit 5 + in-memory H2"]:::be
    fe["Frontend<br/>setup-node 22 · ng build · ng test"]:::fe
  end
  ci --> be
  ci --> fe
  be --> ok([✓ green checks]):::user
  fe --> ok

  classDef user fill:#1e2435,stroke:#1e2435,color:#fff;
  classDef fe fill:#ece8ff,stroke:#7c5cff,color:#1e2435;
  classDef be fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef accent fill:#fff4e0,stroke:#f5b400,color:#1e2435;
  classDef ext fill:#eef2fb,stroke:#9aa3b8,color:#1e2435;
```

The backend job needs **no database** — tests run against in-memory H2. The frontend job runs
Vitest in jsdom (no browser needed).

---

## Container images

Both apps ship as containers (used by every cloud target):

| Image | Dockerfile | Base | Serves |
|---|---|---|---|
| Backend | `backend/Dockerfile` | `maven` → `eclipse-temurin:21-jre` | the Spring Boot jar on **:8585** |
| Frontend | `frontend/angular-ecommerce/Dockerfile` | `node` → `nginx:alpine` | the static SPA on **:80** |

> The frontend's API URL is baked in at build time. Pass it per environment:
> `docker build --build-arg API_URL=https://your-backend-host/api ...` (the deploy workflows do this
> from the `BACKEND_URL` repo variable).

---

## From local to the cloud

A cloud deploy is the [local container deploy](#deploy-locally-full-stack-in-containers) with three
things swapped for managed services. Same images, same env vars — different endpoints:

| Local (`compose.yaml`) | Cloud equivalent |
|---|---|
| Images built by Compose | Pushed to a **container registry** (Artifact Registry / ECR / ACR) |
| `mysql` container | A **managed MySQL 8** (Cloud SQL / RDS / Azure DB for MySQL), empty to start |
| `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/...` | `…=jdbc:mysql://<managed-host>:3306/full-stack-ecommerce` |
| `API_URL=http://localhost:8585/api` build-arg | `API_URL=https://<backend-public-url>/api` (the `BACKEND_URL` repo var) |
| `docker compose up` | A **serverless container service** (Cloud Run / App Runner / Container Apps) |

Everything else is identical, which is the point of containerising: **what you verified locally is what
runs in the cloud.** Two things carry straight over:

- **No manual schema step.** Point the backend at an *empty* managed database and **Flyway migrates it
  on first boot** (then `ddl-auto=validate` guards it); the `DataLoader` seeds the catalog. You do *not*
  run a SQL script by hand.
- **Health & lifecycle.** Wire the platform's health checks to the actuator probes
  (`/actuator/health/readiness`, `/actuator/health/liveness`) and send `SIGTERM` on rollout — the
  backend image handles it as a **graceful shutdown** (drains in-flight requests). Set
  `SPRING_PROFILES_ACTIVE=prod` for quieter logs, JSON logging, and the API docs locked off.

---

## Cloud deployments

All three follow the same shape — **build images → push to the cloud's registry → roll out to a
serverless container service → talk to a managed MySQL.** Pick whichever cloud you like; they're
independent.

### Google Cloud (Cloud Run)
`.github/workflows/deploy-gcp.yml`

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8'}}}%%
flowchart LR
  gha["GitHub Actions<br/>deploy-gcp.yml (manual)"]:::accent -->|docker push| ar["Artifact Registry"]:::ext
  ar --> crf["Cloud Run<br/>frontend · nginx :80"]:::fe
  ar --> crb["Cloud Run<br/>backend · :8585"]:::be
  user([🌍 User]):::user --> crf
  crf -->|"/api"| crb
  crb -->|JDBC| sql[("Cloud SQL<br/>MySQL 8")]:::db
  classDef user fill:#1e2435,stroke:#1e2435,color:#fff;
  classDef fe fill:#ece8ff,stroke:#7c5cff,color:#1e2435;
  classDef be fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef db fill:#fff4e0,stroke:#f5b400,color:#1e2435;
  classDef accent fill:#eaf2ff,stroke:#4285f4,color:#1e2435;
  classDef ext fill:#eef2fb,stroke:#9aa3b8,color:#1e2435;
```

### AWS (App Runner)
`.github/workflows/deploy-aws.yml`

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8'}}}%%
flowchart LR
  gha["GitHub Actions<br/>deploy-aws.yml (manual)"]:::accent -->|docker push| ecr["ECR"]:::ext
  ecr -->|auto-deploy| arf["App Runner<br/>frontend :80"]:::fe
  ecr -->|auto-deploy| arb["App Runner<br/>backend :8585"]:::be
  user([🌍 User]):::user --> arf
  arf -->|"/api"| arb
  arb -->|JDBC| rds[("RDS<br/>MySQL 8")]:::db
  classDef user fill:#1e2435,stroke:#1e2435,color:#fff;
  classDef fe fill:#ece8ff,stroke:#7c5cff,color:#1e2435;
  classDef be fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef db fill:#fff4e0,stroke:#f5b400,color:#1e2435;
  classDef accent fill:#fff1e0,stroke:#ff9900,color:#1e2435;
  classDef ext fill:#eef2fb,stroke:#9aa3b8,color:#1e2435;
```

### Azure (Container Apps)
`.github/workflows/deploy-azure.yml`

```mermaid
%%{init: {'theme':'base','themeVariables':{'fontFamily':'DM Sans, system-ui, sans-serif','lineColor':'#9aa3b8'}}}%%
flowchart LR
  gha["GitHub Actions<br/>deploy-azure.yml (manual)"]:::accent -->|docker push| acr["Container Registry"]:::ext
  acr -->|az containerapp update| caf["Container App<br/>frontend :80"]:::fe
  acr -->|az containerapp update| cab["Container App<br/>backend :8585"]:::be
  user([🌍 User]):::user --> caf
  caf -->|"/api"| cab
  cab -->|JDBC| db[("Azure DB<br/>for MySQL")]:::db
  classDef user fill:#1e2435,stroke:#1e2435,color:#fff;
  classDef fe fill:#ece8ff,stroke:#7c5cff,color:#1e2435;
  classDef be fill:#d3f3ef,stroke:#10b6a6,color:#1e2435;
  classDef db fill:#fff4e0,stroke:#f5b400,color:#1e2435;
  classDef accent fill:#e8f0ff,stroke:#0078d4,color:#1e2435;
  classDef ext fill:#eef2fb,stroke:#9aa3b8,color:#1e2435;
```

---

## Walkthrough: GCP Cloud Run, end to end

A concrete, copy-pasteable run-through of the **one-time setup** behind `deploy-gcp.yml`. (AWS App
Runner and Azure Container Apps follow the same shape with their own CLIs.) Replace the CAPITALISED
values with your own.

**1 — Project, APIs, registry**
```bash
gcloud config set project YOUR_PROJECT_ID
gcloud services enable run.googleapis.com artifactregistry.googleapis.com sqladmin.googleapis.com
gcloud artifacts repositories create luv2shop --repository-format=docker --location=us-central1
```

**2 — Managed MySQL (Cloud SQL), started empty**
```bash
gcloud sql instances create luv2shop-db --database-version=MYSQL_8_0 --tier=db-f1-micro --region=us-central1
gcloud sql databases create full-stack-ecommerce --instance=luv2shop-db
gcloud sql users create ecommerceapp --instance=luv2shop-db --password=CHOOSE_A_PASSWORD
```
No schema script — **Flyway migrates the empty DB on the backend's first boot.** (For connectivity,
use the Cloud SQL connector or an authorized network; see the Cloud Run + Cloud SQL docs.)

**3 — Service account for GitHub Actions**
```bash
PROJ=YOUR_PROJECT_ID; SA="gh-deployer@$PROJ.iam.gserviceaccount.com"
gcloud iam service-accounts create gh-deployer
for r in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser; do
  gcloud projects add-iam-policy-binding $PROJ --member="serviceAccount:$SA" --role="$r"
done
gcloud iam service-accounts keys create key.json --iam-account="$SA"   # paste contents -> GCP_SA_KEY
```

**4 — GitHub secrets & variables** (repo *Settings → Secrets and variables → Actions*)
- Secrets: `GCP_SA_KEY` (the `key.json`), `DB_URL` (`jdbc:mysql://HOST:3306/full-stack-ecommerce`),
  `DB_USER`, `DB_PASS`.
- Variable: `BACKEND_URL` — leave empty for now.

**5 — First deploy (two passes — the chicken-and-egg)**
1. Edit `deploy-gcp.yml`'s `env:` (`GCP_PROJECT`, `GCP_REGION`, `AR_REPO`); commit.
2. **Actions → Deploy · GCP → Run workflow.** The backend deploys and migrates the DB.
3. Copy the backend's Cloud Run URL into the `BACKEND_URL` variable.
4. **Run the workflow again** — the frontend now builds against the live API. Subsequent deploys are a
   single run.

> Add `SPRING_PROFILES_ACTIVE=prod` to the backend's `--set-env-vars` for production logging + locked
> API docs, and point Cloud Run's health check at `/actuator/health/readiness`.

---

## What you configure

Each deploy workflow has an `env:` block with `# <-- EDIT` markers, plus secrets/variables to add
under **repo Settings → Secrets and variables → Actions**.

| Cloud | Edit in the workflow | Secrets | Variables |
|---|---|---|---|
| **GCP** | `GCP_PROJECT`, `GCP_REGION`, `AR_REPO` | `GCP_SA_KEY`, `DB_URL`, `DB_USER`, `DB_PASS` | `BACKEND_URL` |
| **AWS** | `AWS_REGION` | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_ACCOUNT_ID` | `BACKEND_URL` |
| **Azure** | `ACR_NAME`, `RESOURCE_GROUP` | `AZURE_CREDENTIALS` | `BACKEND_URL` |

**Common steps for any cloud:**
1. Create the registry + the two container services + a managed MySQL (an **empty**
   `full-stack-ecommerce` database — no schema script needed).
2. Set the backend's env vars on its service: `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`,
   `SPRING_DOCKER_COMPOSE_ENABLED=false`, and `SPRING_PROFILES_ACTIVE=prod`.
3. On first boot the backend **migrates the empty DB with Flyway** (V1→V2) and the `DataLoader` seeds
   the catalog — there is **no manual SQL step**. Point the platform's health check at
   `/actuator/health/readiness`.
4. Deploy the **backend first**, copy its public URL into the `BACKEND_URL` repo variable, then deploy
   the frontend (so the SPA is built pointing at the live API).
5. (Optional) set `STRIPE_SECRET_KEY` and the Okta issuer env vars to light up payments / auth.

> **Chicken-and-egg note:** the frontend bakes in `BACKEND_URL` at build time, so the very first
> deploy is two passes (backend → set variable → frontend). After that, a single run does both.
