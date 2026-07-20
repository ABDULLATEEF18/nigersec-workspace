# NigerSec — Unified Cybersecurity Intelligence Platform

Nigeria's first open-source cybersecurity platform for both everyday citizens and financial institutions.

- **Citizens** can check if their email, phone number, BVN, or NIN has appeared in a known data breach.
- **Institutions** (banks, fintechs, telecoms) get a real-time threat intelligence feed, peer-shared breach reports, NDPA 2023 compliance reports, and a fraud-scoring API they can embed directly into their payment systems.

---

## Table of Contents

1. [What the Platform Does](#1-what-the-platform-does)
2. [How It's Built](#2-how-its-built)
3. [Project Structure](#3-project-structure)
4. [Quick Start — Run Locally in 3 Steps](#4-quick-start--run-locally-in-3-steps)
5. [Environment Variables](#5-environment-variables)
6. [API Reference](#6-api-reference)
7. [Demo Test Data](#7-demo-test-data)
8. [Running in Production](#8-running-in-production)
9. [Security Design](#9-security-design)
10. [Known Limitations & Roadmap](#10-known-limitations--roadmap)

---

## 1. What the Platform Does

### For Citizens (`/` and `/citizen`)

| Feature | Description |
|---|---|
| **Breach Check** | Enter your email, phone, BVN, or NIN. The backend hashes it and checks against known Nigerian breach databases and an external dark-web API. Your raw data never leaves the server unprotected. |
| **"Notify Me" monitoring** | Subscribe your email to get alerted whenever it appears in a new breach. Requires a citizen account. |
| **Citizen Dashboard** | See all your active monitoring subscriptions, alerts, and check history after logging in. |
| **AI Security Advisor** | Optional Gemini AI assistant that explains breach results and gives you specific action steps (BVN freeze, SIM-swap protection, etc.). |

### For Institutions (`/institution`)

| Feature | Description |
|---|---|
| **Real-time Threat Feed** | Live anonymised feed of cyberattack reports submitted by peer institutions — phishing campaigns, SIM-swap clusters, credential stuffing attacks, etc. |
| **Submit Threat Reports** | Report an attack your institution experienced. The reporter's identity is stripped before it is broadcast to the network. |
| **Fraud Scoring API** | Score any financial transaction in real time. Returns a risk score (0–100), a risk level (LOW / MEDIUM / HIGH), a decision (APPROVE / REVIEW / BLOCK), and the specific rules that fired. |
| **NDPA 2023 Compliance Report** | Generate a monthly compliance report showing how many incidents were reported and whether 72-hour disclosure obligations have been met. |
| **API Key Management** | Issue, list, and revoke API keys so developers can integrate the fraud-scoring endpoint into payment systems. |

---

## 2. How It's Built

```
┌─────────────────────────────────────────┐
│              Browser (React)            │
│  /  Home  ·  /citizen  ·  /institution  │
└──────────────┬──────────────────────────┘
               │  HTTP/JSON  (Bearer JWT)
               ▼
┌─────────────────────────────────────────┐
│     Spring Boot 3.3 — port 8080         │
│     Context path: /api/v1               │
│                                         │
│  Auth  ·  Citizen  ·  Institution       │
│  Fraud  ·  Rate-limit filter            │
└──────┬──────────┬──────────┬────────────┘
       │          │          │
   PostgreSQL   Redis     Kafka
   (data)      (cache +   (events)
               rate limit)
```

### Frontend

| Item | Detail |
|---|---|
| Framework | React 19 |
| Router | react-router-dom v7 |
| Build tool | Vite 8 (Rolldown bundler) |
| Styling | Inline CSS-in-JS (no external UI library) |
| AI (optional) | Google Gemini 2.0 Flash — client-side call |

### Backend

| Item | Detail |
|---|---|
| Framework | Spring Boot 3.3.5 |
| Language | Java 21 |
| Security | Spring Security 6 · JJWT 0.12.6 · BCrypt |
| Database | PostgreSQL 16 via Spring Data JPA / Hibernate |
| Cache | Redis 7 via Spring Cache (Lettuce driver) |
| Messaging | Apache Kafka 3.8 (KRaft mode — no ZooKeeper) |
| Code generation | Lombok 1.18.38 |
| External API | BreachDirectory (RapidAPI) for dark-web lookups |

---

## 3. Project Structure

```
NigerSec-Workspace/
├── docker-compose.yml              ← Starts PostgreSQL, Redis, Kafka
│
├── nigersec-frontend/              ← React SPA
│   ├── src/
│   │   ├── App.jsx                 ← Route definitions (/, /citizen, /institution)
│   │   └── pages/
│   │       ├── Home.jsx            ← Public landing — breach checker + fraud demo
│   │       ├── CitizenDashboard.jsx← Citizen portal (login required)
│   │       └── InstitutionalDashboard.jsx ← Institution portal (login required)
│   ├── .env                        ← Local env vars (VITE_API_URL, VITE_GEMINI_API_KEY)
│   └── vite.config.js              ← Dev proxy: /api → localhost:8080
│
└── nigersec-backend/
    └── intelligence-backend/
        ├── pom.xml
        ├── start-prod.sh           ← One-command production start
        ├── seed-institution.sql    ← SQL to create a real institution row
        └── src/main/java/com/nigersec/intelligence_backend/
            ├── auth/               ← Register, login, JWT, refresh tokens
            ├── citizen/            ← Breach check, monitoring subscriptions
            ├── institution/        ← Threat feed, compliance reports
            ├── fraud/              ← Fraud scoring engine, API key management
            ├── messaging/          ← Kafka event publishers + consumer
            ├── security/           ← JWT filter, token provider
            └── config/             ← CORS, Redis, Kafka topics, rate limiter, Jackson
```

---

## 4. Quick Start — Run Locally in 3 Steps

### Prerequisites

Make sure these are installed:

```bash
docker --version   # Docker 24+
java -version      # JDK 21+
node --version     # Node 18+
```

---

### Step 1 — Start the infrastructure

From the workspace root:

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16** on port `5432`
- **Redis 7** on port `6379`
- **Kafka 3.8** on port `9092`

Wait about 15 seconds for Kafka to be fully ready before the next step.

---

### Step 2 — Start the backend

```bash
cd nigersec-backend/intelligence-backend

JAVA_HOME=/home/blackwrld04/.jdks/openjdk-26 \
PATH=/home/blackwrld04/.jdks/openjdk-26/bin:$PATH \
./mvnw spring-boot:run
```

> **First boot:** The `DataSeeder` automatically creates 5 sample breach records and a demo institution so you can explore the app immediately. See [Demo Test Data](#7-demo-test-data).

Wait until you see this line in the log:
```
Started IntelligenceBackendApplication
```

The backend is now running at **http://localhost:8080/api/v1**

---

### Step 3 — Start the frontend

Open a new terminal:

```bash
cd nigersec-frontend
npm install      # first time only
npm run dev
```

Open **http://localhost:5173** in your browser.

> Vite automatically proxies all `/api/*` requests to `localhost:8080` so you don't need to configure anything.

---

## 5. Environment Variables

### Frontend (`nigersec-frontend/.env`)

```env
# Optional — defaults to /api/v1 (localhost proxy in dev)
VITE_API_URL=

# Optional — enables AI breach advisor and threat explainer
# Get a free key at https://aistudio.google.com/app/apikey
VITE_GEMINI_API_KEY=
```

### Backend — all have safe local defaults

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `nigersec` | Database name |
| `DB_USER` | `nigersec` | DB username |
| `DB_PASS` | `nigersec` | DB password — **change in production** |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(empty)* | Redis password if auth is enabled |
| `JWT_SECRET` | *(insecure default)* | **Must be changed in production** — use `openssl rand -hex 32` |
| `KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `DARK_WEB_API_KEY` | *(empty)* | Optional — enables real dark-web breach lookups via BreachDirectory |
| `FRONTEND_URL` | *(empty)* | Extra CORS origin for production frontend |
| `SPRING_PROFILES_ACTIVE` | *(dev/demo mode)* | Set to `prod` to disable demo data and tighten schema validation |

---

## 6. API Reference

All endpoints are prefixed with `/api/v1`.

### Authentication — no token needed

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth/register` | Create an account. Body: `{ email, password, role, institutionId? }` |
| `POST` | `/auth/login` | Login. Returns `accessToken` (24h) + `refreshToken` (7 days). |
| `POST` | `/auth/refresh` | Exchange a refresh token for a new access token. |
| `POST` | `/auth/logout` | Revoke all refresh tokens for the current user. |

**Roles:** `CITIZEN` · `INSTITUTION` · `DEVELOPER` · `ADMIN`

---

### Citizen — public breach check (no token needed)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/citizen/breach/check` | Check an identifier for breach exposure. Body: `{ identifier, dataType }` where `dataType` is `EMAIL`, `PHONE`, `BVN`, or `NIN`. Rate limited to **10 checks per hour per IP**. |

**Example request:**
```bash
curl -X POST http://localhost:8080/api/v1/citizen/breach/check \
  -H "Content-Type: application/json" \
  -d '{"identifier":"test@example.com","dataType":"EMAIL"}'
```

**Example response:**
```json
{
  "success": true,
  "data": {
    "breached": true,
    "breachCount": 2,
    "breaches": [
      {
        "source": "Flutterwave Data Breach — Apr 2024",
        "exposedFields": "email,name,phone,bank_account",
        "severity": "HIGH",
        "breachDate": "2024-04-15T00:00:00Z",
        "action": "Change your password immediately and enable 2FA on all financial accounts."
      }
    ],
    "recommendation": "Your data has been exposed. Immediately change passwords and enable 2FA."
  }
}
```

---

### Citizen monitoring — requires `CITIZEN` JWT

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/citizen/monitoring/subscribe` | Subscribe an identifier to ongoing breach monitoring. |
| `GET` | `/citizen/monitoring` | List all your active monitoring subscriptions. |
| `DELETE` | `/citizen/monitoring/{id}` | Cancel a specific subscription. |

---

### Institution — requires `INSTITUTION` or `ADMIN` JWT

All these endpoints also require the `X-Institution-Id` header set to your institution's UUID.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/institution/threat-reports` | Submit an anonymised threat report. |
| `GET` | `/institution/threat-feed` | Paginated threat intelligence feed. Optional filters: `?attackType=PHISHING&severity=CRITICAL` |
| `GET` | `/institution/threat-feed/critical` | Top 5 latest critical alerts — used for dashboard header. |
| `GET` | `/institution/compliance-report/{id}?year=2026&month=7` | Generate an NDPA 2023 monthly compliance report. |

**Attack types:** `PHISHING` · `SOCIAL_ENGINEERING` · `DATA_BREACH` · `RANSOMWARE` · `ACCOUNT_TAKEOVER` · `SIM_SWAP` · `INSIDER_THREAT` · `API_ABUSE` · `DDOS` · `CREDENTIAL_STUFFING` · `OTHER`

---

### Fraud API — requires `INSTITUTION`, `DEVELOPER`, or `ADMIN` JWT

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/fraud/score` | Score a transaction in real time. Returns risk score, level, and decision. |
| `GET` | `/fraud/history/{institutionId}` | Paginated fraud signal history. |
| `POST` | `/fraud/api-keys` | Issue a new API key. The raw key is shown **once only**. |
| `GET` | `/fraud/api-keys/{institutionId}` | List active API keys (hashed IDs, no raw keys). |
| `DELETE` | `/fraud/api-keys/{keyId}` | Revoke an API key immediately. |

**Fraud score request:**
```bash
curl -X POST http://localhost:8080/api/v1/fraud/score \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "X-Institution-Id: <uuid>" \
  -d '{
    "transactionId": "txn-001",
    "senderAccount": "0123456789",
    "receiverAccount": "9876543210",
    "amount": 750000,
    "channel": "USSD",
    "transactionTime": "2026-07-20T02:30:00Z"
  }'
```

**Fraud score rules (Phase 1):**

| Rule | Trigger | Points |
|---|---|---|
| Breach exposure | Sender account in known breach records | +30 |
| BVN history | BVN linked to prior HIGH-risk signals | +35 |
| High velocity | More than 5 transactions from same account in 1 hour | +20 |
| Large amount | Transaction above ₦500,000 | +15 |
| Off-hours | Transaction between midnight and 5 AM WAT | +10 |
| USSD + large | USSD channel with amount above ₦100,000 | +10 |

Score 0–39 → **APPROVE** · 40–69 → **REVIEW** · 70–100 → **BLOCK**

---

## 7. Demo Test Data

When running in development mode (default), these test identifiers are pre-loaded into the database and will return breach results immediately:

| Type | Identifier | Expected result |
|---|---|---|
| Email | `test@example.com` | 2 breaches — Flutterwave (HIGH) + Jumia (MEDIUM) |
| Phone | `2348012345678` | 1 breach — MTN Nigeria (CRITICAL) |
| BVN | `12345678901` | 1 breach — Dark Web Financial Dump (CRITICAL) |
| NIN | `98765432101` | 1 breach — NIMC Contractor (HIGH) |
| Any clean value | e.g. `nobody@clean.com` | No breach found |

A demo institution is also created automatically:

| Field | Value |
|---|---|
| Name | First Bank of Nigeria (Demo) |
| UUID | Printed in the Spring Boot startup log |

Use this UUID as the Institution ID when registering an institution user account on the portal.

---

## 8. Running in Production

Use the provided script — it handles everything:

```bash
cd nigersec-backend/intelligence-backend
bash start-prod.sh
```

The script will:
1. Auto-generate a secure `JWT_SECRET` (print it and save it — you need the same secret across restarts)
2. Prompt for your PostgreSQL credentials if not already set as environment variables
3. Ask for your frontend URL to add to the CORS allowlist
4. Start the backend with `SPRING_PROFILES_ACTIVE=prod`, which:
   - Disables `DataSeeder` (no fake data is inserted)
   - Sets `ddl-auto: validate` (Hibernate won't modify your schema — safe for production data)
   - Reduces log level to WARN

### Creating your first institution in production

The demo institution seeder doesn't run in prod mode. You need to insert one manually using the SQL file:

```bash
# Edit the values in the file first
nano nigersec-backend/intelligence-backend/seed-institution.sql

# Run it against your database
psql -h localhost -U nigersec -d nigersec \
  -f nigersec-backend/intelligence-backend/seed-institution.sql
```

The script prints the generated institution UUID:
```
NOTICE: Institution ID (give this to your users): a1b2c3d4-...
```

Give that UUID to anyone who registers as an institution user on the portal.

### Building the frontend for production

```bash
cd nigersec-frontend

# Set your production API URL
echo "VITE_API_URL=https://api.yourdomain.com/api/v1" > .env

npm run build
# Output is in dist/ — serve it with nginx, Caddy, or any static host
```

---

## 9. Security Design

### Zero-knowledge breach checks

When you submit an identifier for a breach check, the backend hashes it with **SHA-256** before querying the database. Your raw BVN, NIN, phone number, or email is never stored or logged.

For email lookups via the external BreachDirectory API, a **k-anonymity** model is used — only the first 5 characters of the SHA-1 hash are sent to the external service, so the exact email is never revealed.

### Authentication

- **Access tokens** expire after 24 hours (JWT, signed with HMAC-SHA256)
- **Refresh tokens** expire after 7 days and are stored in PostgreSQL
- On login, all previous refresh tokens for that account are revoked (rotating token model)
- Passwords are hashed with **BCrypt** before storage

### Rate limiting

The public breach check endpoint is limited to **10 requests per hour per IP address**, enforced via Redis counters.

### Role-based access

Every protected endpoint checks the JWT role claim:

| Role | Access |
|---|---|
| `CITIZEN` | Own monitoring subscriptions only |
| `INSTITUTION` | Threat feed, compliance reports, fraud scoring |
| `DEVELOPER` | Fraud scoring API only |
| `ADMIN` | Everything |

---

## 10. Known Limitations & Roadmap

### Current limitations

- **Phone, BVN, NIN** are sent in plaintext to the external BreachDirectory API. Only email uses k-anonymity. A fix is planned to skip external lookups for these sensitive types and rely on the local database only.
- **Token refresh** is implemented but the frontend automatically handles this — expired sessions are cleared and the user is redirected to login.
- **Hotspots map** and **BVN batch tracker** panels in the institution dashboard show mock data — the backend endpoints for these are not yet implemented.
- The **fraud scoring engine** uses rule-based logic (Phase 1). It is designed to be replaced with a trained ML model once the `fraud_signals` table exceeds 1,000,000 records.

### Roadmap (Phase 2)

- [ ] ML-based fraud scoring model trained on real signal data
- [ ] Email notification system for breach alerts (SendGrid / Mailgun)
- [ ] Webhook support so institutions can receive fraud signals in real time
- [ ] k-anonymity for phone/BVN/NIN lookups
- [ ] Admin dashboard for managing institutions and users
- [ ] Dedicated `/stats` endpoint for live platform statistics on the home page

---

## Running the smoke tests

After starting the backend, verify everything works end-to-end:

```bash
# 1. Health check
curl http://localhost:8080/api/v1/actuator/health
# → {"status":"UP"}

# 2. Breach check — should return 2 breaches
curl -s -X POST http://localhost:8080/api/v1/citizen/breach/check \
  -H "Content-Type: application/json" \
  -d '{"identifier":"test@example.com","dataType":"EMAIL"}'

# 3. Register a citizen account
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@test.com","password":"password123","role":"CITIZEN"}'

# 4. Login — copy the accessToken from the response
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@test.com","password":"password123"}'

# 5. Reset rate limit if you hit the 10/hr cap
redis-cli del "rate:breach:127.0.0.1"
```

---

*NigerSec is built to protect Nigerian citizens and institutions against the growing wave of data breaches and financial fraud. NDPA 2023 compliant.*
