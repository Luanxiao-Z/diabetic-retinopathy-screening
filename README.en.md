# Diabetic Retinopathy (DR) Screening System

[![CI](https://github.com/Luanxiao-Z/diabetic-retinopathy-screening/actions/workflows/ci.yml/badge.svg)](https://github.com/Luanxiao-Z/diabetic-retinopathy-screening/actions/workflows/ci.yml)

**English** | [简体中文](README.md)

An **open-source learning project** ([GitHub](https://github.com/Luanxiao-Z/diabetic-retinopathy-screening), [MIT License](LICENSE)) for AI-assisted fundus screening. It ships a complete frontend, backend, model inference service and containerized deployment — a practical engineering reference for medical-AI work. Upload a fundus photograph and the system performs five-level DR grading (LEVEL_0 normal to LEVEL_4 proliferative), generates a Grad-CAM heatmap, and suggests a referral action; it also provides record management, uncertainty-driven human review, longitudinal patient follow-up, analytics and printable reports.

The repository contains a runnable full stack; model weights and training metrics are versioned, so a clone plus the setup in [Section 6](#6-quick-start) is enough to run locally or in containers.

### Why it matters

Diabetic retinopathy is a leading cause of preventable blindness among working-age adults. It is usually asymptomatic in its early stages, yet most severe vision loss is avoidable **if detected and treated before the proliferative stage**. Primary care settings, however, generally lack ophthalmologists and grading capacity — so many patients miss the intervention window.

This system automates five-level grading and the referral decision at the screening stage, **reducing dependence on specialists and improving consistency**. Uncertain predictions are routed to a clinician for review (human-in-the-loop) using **normalized predictive entropy**, and Grad-CAM heatmaps make the model's attention traceable for doctor–patient communication and teaching.

> **Scope and disclaimer**: this is a teaching and research demo, **not a medical device**, and has not been clinically validated or regulatory-approved. AI output is **for research and educational reference only** and must not be used for diagnosis, treatment decisions, or as a substitute for a licensed physician. Dataset and third-party components follow their own licenses (see [Section 11](#11-dataset) and [Section 16](#16-license)).

---

## 1. Features

### Screening workflow
- **Upload**: images are queued for preview before inference, then processed one by one; a failed image does not block the batch and can be retried individually, with live status and latency per image
- **Records**: search by patient, grade, review status and date; review status distinguishes "pending / confirmed / not required"; batch delete and Excel export; auto-fitted column widths, draggable and resettable headers; **double-click a row to open the detail page**
- **Follow-up todos**: paginated table aggregating three task types — pending review, referral required (severe or worse), overdue (>90 days with moderate or worse); filter by type and patient name
- **Patient follow-up**: screening histories merged per patient, showing grade change (previous → latest) and trend (worsening / improving / stable / first visit) plus a visit timeline
- **Human review**: results whose **normalized entropy** exceeds **0.15** require clinician confirmation, with the review comment retained for traceability
- **Diagnostic report**: A4 layout with patient info, original and Grad-CAM images, grading conclusion and per-class probabilities, review record and disclaimer; printable or saveable as PDF

### Dashboards
- **Workbench**: four KPIs (total screenings, referral rate, referrals, pending review) with period-over-period comparison, a 14-day screening volume chart with Y-axis ticks, a model status card, a to-do summary and quick entries
- **Analytics**: statistics table (total / last 30 days / previous 30 days / growth / referral rate / per-suggestion counts / pending review), grade and suggestion distributions, and a 30-day trend
- **Model info**: version, backbone, inference device and training metrics (accuracy, macro F1, per-class F1, per-epoch curves)

### Administration
- **User management**, **dictionary management** (domains and items), **operation logs** (login, upload, delete, export, review, account changes — with actor, result, latency and IP)

### Common capabilities
- **Registration**: self-service sign-up grants the DOCTOR role with self-only data scope; unique username, password must contain letters and digits; protected by **two-layer rate limiting** (5 per IP per 10 minutes, 30 per site per minute)
- **Login security**: image captcha (5-minute validity, single use) plus **brute-force protection** (10 failed attempts per IP within 5 minutes → HTTP 429 envelope, cleared on success)
- **Permissions**: lightweight role mapping (ADMIN / DOCTOR) with ALL / SELF data scope — no RBAC tables
- **Multi-tab workspace**: tab state survives switching and page reload; tabs carry a patient suffix (e.g. `Record detail-T001`) when the same page is opened for different patients
- **Compact filters**: labels inline with controls, single row, widths shrink proportionally when space is tight
- **Full-viewport modals**: dialogs, drawers and image previews are teleported to `body`, so the overlay covers the sidebar and top bar too
- **Personal center**: profile editing (name, phone) and password change

### Screenshots

**Screening**

![Upload](docs/images/screening-upload.png)
*Upload: queue, preview, then infer one by one*

![Records](docs/images/screening-records.png)
*Records: filter by patient / grade / review status, batch delete and export*

![Record detail](docs/images/screening-detail.png)
*Record detail: structured result on the left, A4 report preview on the right*

![Report](docs/images/screening-report.png)
*Diagnostic report: original fundus, Grad-CAM heatmap, grading and probabilities*

![Todos](docs/images/screening-todos.png)
*Follow-up todos: pending review / referral / overdue*

![Patients](docs/images/screening-patients.png)
*Patient follow-up: grade change, trend and visit timeline*

**Dashboards**

![Workbench](docs/images/dashboard.png)
*Workbench: KPIs, 14-day volume and to-do summary*

![Analytics](docs/images/screening-statistics.png)
*Analytics: statistics table, distributions and 30-day trend*

![Model info](docs/images/model-info.png)
*Model info: backbone, device and training metrics*

**Administration**

![Users](docs/images/admin-users.png)
*User management*

![Dictionaries](docs/images/admin-dicts.png)
*Dictionary management*

![Logs](docs/images/admin-logs.png)
*Operation logs*

**Common**

![Login](docs/images/login.png)
*Login with captcha*

![Profile](docs/images/profile.png)
*Personal center*

![Guide](docs/images/guide.png)
*User guide*

![About](docs/images/about.png)
*About*

![Tabs](docs/images/interaction-tabs.png)
*Multi-tab management: patient suffixes, refresh and bulk close in the tab bar*

![Modal](docs/images/interaction-dialog.png)
*Modals and previews: overlay covers the entire viewport*

> **Time format**: UI timestamps use `yyyy-MM-dd HH:mm:ss` (space-separated, no `T`); only troubleshooting-oriented places such as operation logs keep ISO 8601.

---

## 2. Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Vue 3.5 + TypeScript 5.6 + Vite 6 + Element Plus 2.9 + Pinia 2.3 + Vue Router 4.5 + ECharts 5.5 (Yarn 1.22, native CSS design tokens) |
| Backend | JDK 21 + Spring Boot 3.4.4 + Maven multi-module + MyBatis-Plus 3.5.11 + Redis 7 (Jedis, token sessions) + Knife4j 4.5 + Apache POI |
| Model service | **Python 3.13+** + FastAPI + PyTorch 2.14 (cu130 / CPU) + torchvision 0.29 + MobileNetV3-Small + custom Grad-CAM |
| Storage | MySQL 8 (business data), Redis 7 (sessions), **Silo** (MinIO-compatible fork, private bucket `dr-screening`) |
| Deployment | Docker + Docker Compose + Nginx 1.27; separate images for web, backend and model service |

> Detailed version constraints and rationale: [`docs/design/项目技术栈.md`](docs/design/项目技术栈.md) (Chinese).

---

## 3. Architecture

```mermaid
flowchart TD
    User([Doctor / Administrator]) -->|HTTP / HTTPS| Nginx[Nginx<br/>reverse proxy + static assets]

    Nginx -->|static| Web[Frontend<br/>Vue3 + Vite<br/>port 5173]
    Nginx -->|/api/v1 REST| Backend

    subgraph Backend[Spring Boot multi-module · port 8080]
        Auth[Security<br/>token auth + captcha]
        System[System<br/>users / dictionaries / logs]
        Screening[Screening<br/>upload / records / review / follow-up / stats]
    end

    Backend -->|MyBatis-Plus| MySQL[(MySQL 8<br/>business data · 3306)]
    Backend -->|Jedis| Redis[(Redis 7<br/>token sessions / captcha · 26379)]
    Backend -->|S3 presigned| Silo[(Silo<br/>private image bucket · 9000 / 9001)]
    Backend -->|HTTP /predict| Model[Model service<br/>FastAPI + PyTorch<br/>port 8000]
    Model -->|PyTorch inference| GPU[(GPU CUDA<br/>optional)]
```

**Key conventions**

- Response envelope is `{ code, msg, data }`; success is `code=200`, business errors keep HTTP 200 with the envelope code (401/403 additionally set the HTTP status)
- Authentication uses **UUID token + Redis session** (not JWT), sent via the `X-Access-Token` header
- Images live in a **private** object bucket; the frontend receives **30-minute presigned URLs**
- Backend-to-model-service calls are plain HTTP; if the model service is unreachable the backend degrades with `503`

---

## 4. Project Structure

```
diabetic-retinopathy-screening/
├── backend/                     Maven aggregate (Java 21)
│   ├── drs-dependencies/        BOM
│   ├── drs-module/
│   │   ├── drs-module-common/   Envelope, exceptions, audit contract, password utils
│   │   ├── drs-module-security/ Auth context, permission annotations
│   │   ├── drs-module-system/   Auth, users, dictionaries, operation logs
│   │   └── drs-module-screening/Screening: inference, records, stats, export
│   └── drs-app/drs-app-server/  Boot module (fat jar)
├── frontend/web/                Vue 3 + Vite (PC only)
│   └── src/{api,components,composables,directives,layouts,router,stores,styles,types,utils,views}
├── model-service/               FastAPI inference service
│   ├── app/                     Pipeline, Grad-CAM, model loading, config
│   ├── models/                  Weights and metrics (versioned)
│   └── training/                Dataset loaders and training entry point
├── datasets/                    APTOS 2019 (not versioned)
├── deploy/                      Nginx and Docker Compose examples
├── docs/                        Specifications, screenshots, model card, third-party notices
├── sql/init/                    MySQL schema and dictionary seed (idempotent)
└── tools/                       Maven wrapper, demo data generation
```

---

## 5. Requirements

| Component | Version |
| --- | --- |
| JDK | 21 |
| Maven | 3.9.x |
| Node.js | 18+ (22 recommended) |
| Yarn | 1.22 |
| Python | 3.13 (matches torch cu130 wheels) |
| MySQL / Redis / Silo | 8.0 / 7 / latest |
| NVIDIA driver + CUDA | optional; the model service falls back to CPU |

---

## 6. Quick Start

### Option 1: one-command startup of all six services (recommended)

If you just want to see the system running, start all six services
(MySQL / Redis / Silo / model service / backend / frontend) with a single command:

```bash
cp deploy/.env.example deploy/.env       # adjust passwords and ports as needed
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile model up -d --build
```

Then open <http://localhost> and sign in with `admin / admin123`.

| Note | Detail |
| --- | --- |
| Services | mysql(3307) / redis(26379) / minio(9000-9001) / model(8000) / backend(8080) / web(80) |
| `--profile model` | The model service is **not** part of the default profile (the image carries PyTorch); pass this flag to start it too |
| First run | ~8–12 minutes (Maven packaging, frontend build, ~200 MB CPU PyTorch download); restarts are seconds |
| Inference device | The container ships **CPU** PyTorch (image ≈ 1.15 GB). For GPU acceleration run the model service on the host instead and bind `0.0.0.0` |
| Persistence | Data lives in the named volumes `mysql-data` / `redis-data` / `minio-data`; `down` keeps them |
| Stop | `docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile model down` |
| Status / logs | `... ps` / `docker logs -f drs-backend` (containers: drs-mysql, drs-redis, drs-minio, drs-model, drs-backend, drs-web) |

> **Password precedence**: `.env` only supplies defaults — **shell environment variables win**. If your machine already
> defines `DB_PASSWORD` / `REDIS_PASSWORD` / `MINIO_ACCESS_KEY`, containers use those values. When debugging
> credentials, trust `docker exec <container> printenv <VAR>` over the `.env` file.

### Option 2: local development (three application services on the host)

> Use this when you need to modify code, debug, or want the model service to use the host GPU.
> The database, cache and object storage can simply reuse the infrastructure containers from Option 1
> (see Prerequisites below) — nothing else needs to be installed.

### 0. Prerequisites

In local development mode the three application services run on the host, so MySQL / Redis / Silo must be up first.
The easiest way is to start only the infrastructure containers from Option 1 (no image build, starts in seconds):

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d mysql redis minio
```

If these services are already installed locally, just confirm the ports below:

```bash
# Linux / macOS
ss -lnt | grep -E ":(3306|26379|9000|9001)"
# Windows
netstat -ano | findstr "3306 26379 9000 9001"
```

### 1. Configure credentials

All connection details are injected via environment variables; **never commit plaintext secrets**:

| Variable | Purpose |
| --- | --- |
| `DB_PASSWORD` | MySQL password (database `dr_screening`, user `root`) |
| `REDIS_PASSWORD` | Redis password |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | Object storage credentials (the private bucket `dr-screening` is created on startup) |

```powershell
# Windows (PowerShell, user-level; open a new terminal afterwards)
setx DB_PASSWORD "your-db-password"
setx REDIS_PASSWORD "your-redis-password"
setx MINIO_ACCESS_KEY "your-access-key"
setx MINIO_SECRET_KEY "your-secret-key"
```

```bash
# Linux / macOS
export DB_PASSWORD=... REDIS_PASSWORD=... MINIO_ACCESS_KEY=... MINIO_SECRET_KEY=...
```

### 2. Initialize the database

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < sql/init/01_init_dr_screening.sql
```

> The script is **idempotent**: create tables, seed dictionaries and apply incremental migrations. On Windows add `--default-character-set=utf8mb4` if you see mojibake.

### 3. Start the model service (port 8000)

```powershell
cd model-service
py -3.13 -m venv .venv            # first time only
. .venv\Scripts\Activate.ps1
pip install -r requirements.txt   # ~3 GB CUDA wheels on first install
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

```bash
# Linux / macOS / Git Bash
cd model-service
python3 -m venv .venv && source .venv/bin/activate   # Git Bash: source .venv/Scripts/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Verify:

```bash
curl http://127.0.0.1:8000/health
# expected: {"status":"UP","model":"aptos2019-mobilenetv3s-1.0.0","device":"cuda","trained":true,...}
```

> **Binding address when running alongside docker-compose (important)**: the backend container reaches the host model
> service via `host.docker.internal:8000`. If the service binds only `127.0.0.1`, containers cannot reach it and uploads
> return `code=503` ("cannot reach inference service"). Bind all interfaces in that case:
>
> ```bash
> uvicorn app.main:app --host 0.0.0.0 --port 8000
> ```
>
> When running locally without containers, keep `127.0.0.1` — it is the safer default.

> Without an NVIDIA GPU, replace the last two lines of `requirements.txt` with the `+cpu` wheels — the service falls back to CPU (`device` reports `cpu`). If `models/best_model.pth` is missing, the service still starts with `trained=false` and random weights, which is **only useful for API debugging**.

### 4. Start the backend (port 8080)

```bash
bash tools/mvn.sh clean install -DskipTests          # build the fat jar (run inside backend/)
cd backend/drs-app/drs-app-server/target
java -jar drs-app-server-1.0.0.jar --server.port=8080
```

Verify:

```bash
curl http://127.0.0.1:8080/api/v1/common/health
# expected: {"code":200,"msg":"success","data":"UP"}
```

> **Stop port 8080 before rebuilding**, otherwise `mvn clean` cannot delete the occupied `target/`.

### 5. Start the frontend (port 5173)

```bash
cd frontend/web
yarn install
yarn dev        # http://localhost:5173, /api proxied to 8080
yarn build      # production build into dist/
```

### 6. Sign in

Open <http://localhost:5173>.

| Account | Password | Role | Data scope |
| --- | --- | --- | --- |
| `admin` | `admin123` | Administrator | All records (ALL) |
| `doctor` | `doctor123` | Doctor | Own records (SELF) |

> Accounts are seeded idempotently at backend startup. Self-service sign-up always creates a DOCTOR with SELF scope; administrator accounts can only be created by an existing administrator.

### 7. Startup order

```
MySQL / Redis / Silo (required first)
          ↓
   model service :8000  ──HTTP──►  backend :8080  ──HTTP──►  frontend :5173
```

There is no hard ordering between the three applications: the backend starts without the model service and only degrades (`code=503`) when inference is requested. Starting in the order "model → backend → frontend" makes verification easier.

### 8. Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| "Cannot reach inference service" on upload | Model service (8000) is down; start it and use "Retry failed" |
| Backend fails to connect to MySQL | MySQL offline, or `DB_PASSWORD` not visible in the current terminal |
| "Invalid username or password" for seed accounts | Seeds are created on the **first** backend start; ensure the schema was initialized |
| Chinese text shows as mojibake (e.g. `DR å††çº§`) | The MySQL client defaulted to latin1 during init, double-encoding the text. The init script now sets `SET NAMES utf8mb4`; for an already-corrupted database, truncate the dictionary tables and re-import with `--default-character-set=utf8mb4` |
| "Login attempts too frequent" | Brute-force protection: 10 failed attempts per IP in 5 minutes; wait for the window to expire |
| "Registration too frequent" | Two-layer rate limit (5 per IP per 10 min, 30 per site per min); retry later |
| Pagination / empty states in English | Element Plus locale not injected (`zhCn` in `main.ts`) |
| `mvn clean` cannot delete `target` | Port 8080 still running; stop the backend first |
| `WinError 32` when importing torch | Windows DLL scan lock; retry (not a broken install) |
| `Failed to load PyTorch C extensions` | venv Python ABI does not match the wheel (e.g. 3.12 venv with cp313 wheels); recreate the venv with 3.13 |

---

## 7. Ports

| Service | Port | Notes |
| --- | --- | --- |
| Frontend (dev) | 5173 | Vite dev server, `/api` proxied to the backend |
| Backend | 8080 | REST API under `/api/v1` |
| Model service | 8000 | FastAPI inference |
| MySQL | 3306 | Business database |
| Redis | 26379 | Overridable via `SPRING_DATA_REDIS_PORT` / `REDIS_PORT` |
| Silo API / Console | 9000 / 9001 | Object storage |

---

## 8. API Overview

All business endpoints live under `/api/v1` and authenticate via `X-Access-Token`.

### Auth and common

| Method | Path | Description |
| --- | --- | --- |
| POST | `/auth/sessions` | Sign in; returns token, role, permissions and data scope (captcha required; IP-scoped brute-force limit) |
| DELETE | `/auth/sessions` | Sign out (invalidate token) |
| GET | `/auth/captcha` | Issue an image captcha (anonymous, 5 minutes, single use) |
| POST | `/auth/users` | Self-service registration (anonymous, IP + global rate limited) |
| GET / PUT | `/common/users/me` | Current profile / update own name and phone |
| PUT | `/common/users/me/password` | Change password |
| GET | `/common/dicts`, `/common/dicts/{code}/items` | Dictionary lookup |

### Screening (`biz:screening:*`)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/biz/screening-records` | Upload a fundus image and run inference (multipart) |
| GET | `/biz/screening-records` | Paged query (respects data scope; `pageSize` clamped to 1–100) |
| GET | `/biz/screening-records/{id}` | Detail with presigned URLs |
| DELETE | `/biz/screening-records/{id}` | Logical delete plus object cleanup |
| PATCH | `/biz/screening-records/{id}/review` | Confirm human review |
| GET | `/biz/screening-records/statistics` | Statistics (distributions, referral rate, trend, growth) |
| GET | `/biz/screening-records/patients` | Patient follow-up aggregation |
| GET | `/biz/screening-records/todos` | Follow-up todos (three task types) |
| GET | `/biz/screening-records/exports` | Excel export |
| GET | `/biz/screening-records/model-info` | Model metadata and training metrics |

### Administration (`admin:*`)

| Method | Path | Description |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/admin/users`, `/admin/users/{id}` | User queries and maintenance |
| PATCH | `/admin/users/{id}/state` | Enable / disable |
| GET/POST/PUT/DELETE | `/admin/dict-domains`, `/admin/dict-domains/{code}/items` | Dictionary maintenance |
| GET | `/admin/operation-logs` | Operation log query |

### Model service (standalone)

| Method | Path | Description |
| --- | --- | --- |
| GET | `/health` | Health check (device, weights loaded) |
| GET | `/model/info` | Model metadata and training metrics |
| POST | `/predict` | Multi-class inference |
| POST | `/cam` | Grad-CAM heatmap (PNG) |

---

## 9. Permission Model

Lightweight role mapping plus data scope — **no RBAC tables**:

| Role | Data scope | Functional permissions |
| --- | --- | --- |
| ADMIN | ALL | `admin:*`, `common:dict:view`, `biz:screening:*` |
| DOCTOR | SELF | `common:dict:view`, `biz:screening:create/view/export/delete/review` |

Data scope is appended as a filter in the service layer; functional permissions are enforced by `@RequirePermission` on the backend and the `v-permission` directive on the frontend, with identical permission codes.

---

## 10. Data Model

| Table | Purpose |
| --- | --- |
| `sys_user` | Users (role, status, password digest) |
| `sys_dict_domain`, `sys_dict_item` | Dictionary domains and items |
| `biz_screening_record` | Screening records (grade, confidence, entropy, per-class probabilities, suggestion, review info, object keys) |
| `sys_operation_log` | Audit log |

Schema and seed: `sql/init/01_init_dr_screening.sql` (idempotent).

---

## 11. Dataset

Trained and evaluated on the **APTOS 2019 Blindness Detection** dataset.

| Item | Detail |
| --- | --- |
| Source | Kaggle competition; images provided by Aravind Eye Hospital, India |
| Task | Five-level DR grading (0 = no DR … 4 = proliferative) |
| Version used | `sovitrath` 224×224 preprocessed release (black borders cropped) |
| Images | 3662 |
| Labels | `train.csv` (`id_code, diagnosis`) |

| Grade | Label | Images | Share |
| --- | --- | --- | --- |
| No DR | 0 | 1805 | 49.3% |
| Mild | 1 | 370 | 10.1% |
| Moderate | 2 | 999 | 27.3% |
| Severe | 3 | 193 | 5.3% |
| Proliferative | 4 | 295 | 8.1% |

Expected layout:

```
datasets/aptos2019_224x224/
├── train.csv
└── colored_images/{No_DR,Mild,Moderate,Severe,Proliferate_DR}/*.png
```

```bash
kaggle datasets download -d sovitrath/diabetic-retinopathy-224x224-2019-data
unzip diabetic-retinopathy-224x224-2019-data.zip -d datasets/
```

`datasets/` is **not versioned**. The dataset is licensed by its original provider for **research and teaching only**; cite as:

> APTOS 2019 Blindness Detection, Kaggle. Data provided by Aravind Eye Hospital, India.

---

## 12. Model

> Training data, performance, limitations, ethical considerations and prohibited uses: see the [Model Card](docs/model-card.md).

| Item | Detail |
| --- | --- |
| Backbone | MobileNetV3-Small (5-class head) |
| Training data | APTOS 2019, 3662 images, stratified 8:1:1 split (seed 42) |
| Entry point | `model-service/training/train.py` |
| Weights | `model-service/models/best_model.pth` (**versioned**, ~5.9 MB) |
| Grade mapping | `LEVEL_0/1 → REVIEW`, `LEVEL_2 → CLINIC`, `LEVEL_3/4 → REFERRAL` |

```bash
cd model-service
python -m training.train --data-root ../datasets/aptos2019_224x224 \
  --epochs 30 --batch-size 32 --lr 3e-4 --resample sqrt --weight-scheme none
```

Test set performance (367 images): **accuracy 0.8229**, **macro F1 0.6528**; best validation epoch is 29–30 (more epochs overfit).

### Class imbalance

The training split is skewed 9.4× between the largest and smallest class. `--resample sqrt` mitigates this at sampling level only (validation/test keep the original distribution). Three-seed experiments:

| Configuration | Accuracy | Macro F1 | Severe NPDR F1 |
| --- | --- | --- | --- |
| None | 0.8056 ± 0.0134 | 0.6450 ± 0.0260 | 0.3502 ± 0.020 |
| **sqrt (used)** | **0.8193 ± 0.0093** | **0.6518 ± 0.0167** | 0.3671 ± 0.093 |
| Oversample | 0.8002 ± 0.0072 | 0.6379 ± 0.0176 | 0.3965 ± 0.089 |

> The severe class has only 20 test images; its F1 standard deviation (±0.09) dwarfs the improvement from resampling, so no significant gain can be claimed. More data — not resampling — is the real lever.

### Uncertainty and review threshold

Uncertainty is the normalized predictive entropy `−Σ pᵢ·ln pᵢ / ln 5` (0 = certain, 1 = uniform). Using confidence instead of entropy is ineffective because softmax saturates:

| Rule | Trigger rate | Error recall |
| --- | --- | --- |
| Confidence < 0.70 | 8.7% | 20.3% (13/64) |
| **Entropy ≥ 0.15 (current)** | **24.5%** | **53.1% (34/64)** |
| Entropy ≥ 0.20 | 20.7% | 45.3% (29/64) |

The threshold lives in `BizScreeningRecordConvert.REVIEW_UNCERTAINTY_THRESHOLD`. **It is bound to the weights — recalibrate after retraining.**

---

## 13. Development and Quality

- **Specifications**: project rules and per-area guides are maintained under [`docs/`](docs/) (Chinese); when they conflict with code, code wins and the docs must be updated
- **Frontend**:

  ```bash
  cd frontend/web
  yarn build       # vue-tsc --noEmit && vite build
  yarn lint        # ESLint
  yarn stylelint   # Stylelint (also inside .vue)
  ```

- **Backend**: unified response envelope, centralized exception handling, Knife4j annotations
- **Model service**: `ruff check app/ training/` (rule set pinned in `model-service/ruff.toml`)
- **CI**: [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs three jobs on push and pull request — backend compile, frontend type-check plus build, model-service static analysis
- **Image publishing**: [`.github/workflows/release-images.yml`](.github/workflows/release-images.yml) builds the `backend` and `web` images on a GitHub runner when a `v*` tag is pushed (or manually triggered) and pushes them to GHCR as `ghcr.io/<owner>/<repo>/{backend,web}`, tagged with both the version and `latest`. The model image (≈1.15 GB CPU) is **not** published from CI — build it locally when needed
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/)
- **Contributing**: see [CONTRIBUTING.md](CONTRIBUTING.md)

---

## 14. Deployment

- **Local**: three processes as described in Option 2 of [Section 6](#6-quick-start); infrastructure reuses the containers from Option 1
- **Prebuilt images (GHCR)**: skip local builds and pull the published artifacts:

  ```bash
  docker pull ghcr.io/luanxiao-z/diabetic-retinopathy-screening/backend:latest
  docker pull ghcr.io/luanxiao-z/diabetic-retinopathy-screening/web:latest
  ```

  > Tags are derived from the Git tag (`v1.0.1` → `1.0.1`, `1.0`, `latest`; the `v` prefix is dropped).
  > Packages of a public repository can be pulled anonymously — no `docker login` required.

  To use them with the compose file, replace the corresponding `build` block with an `image:` reference.
- **Containers**: `deploy/` ships `docker-compose.yml`, three Dockerfiles and an Nginx config. When the model service runs on the host Python, it **must bind `0.0.0.0`** (containers reach it via `host.docker.internal`; a loopback address is unreachable) Default profile starts five services (MySQL / Redis / Silo / backend / frontend); the model service normally runs on the host Python and can be containerized with `--profile model`. Registry mirrors are switchable via `DOCKER_REGISTRY`. See [deploy/README.md](deploy/README.md)

---

## 15. Helper Scripts

| Script | Purpose |
| --- | --- |
| `tools/mvn.sh` | Maven wrapper for Git Bash (handles POSIX path issues) |
| `tools/seed-demo-data.sh` | Idempotent demo data (accounts and screening records without images), `--clean` supported; records are spread across the last 14 days for trend and growth charts |
| `tools/gen-demo-data.py` | Generate demo records with real images and heatmaps through the upload API |

> Screenshot automation, review-threshold calibration and resampling comparison were one-off experiment scripts and are not distributed; their methods and conclusions remain in the Model Card, Section 11/12 and `docs/images/`.

---

## 16. License

Released under the [MIT License](LICENSE). Third-party components keep their own licenses — the full list (version, license, purpose) is in [docs/THIRD-PARTY-NOTICES.md](docs/THIRD-PARTY-NOTICES.md) and inside the running app under *Help → About*. The APTOS 2019 dataset is licensed by its provider for research and teaching only.

> Again: this is a teaching and research demo and **must not be used for clinical diagnosis**.
