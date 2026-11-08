# FaultLens API Contract Specification

This document details the REST API endpoints and real-time WebSocket channels provided by the FaultLens backend application.

- **Base URL:** `http://localhost:3500/api/v1`
- **WebSocket Protocol URL:** `ws://localhost:3500/ws`
- **Swagger Documentation Page:** `http://localhost:3500/swagger-ui/index.html`
- **Machine-readable contract:** [`contracts/openapi.json`](../../contracts/openapi.json) (OpenAPI 3.1, the `/v3/api-docs` document)

### The contract file (P10 / 1.3)

`contracts/openapi.json` is generated from the running API and committed. The frontend generates
its TypeScript types from it (`pnpm gen:api` → `faultlens-frontend/shared/api/schema.d.ts`) and
checks its DTOs against them at compile time (`shared/api/contract.check.ts`).

- Property names are documented as on the wire: swagger-core uses Jackson 2 and does not see the
  Jackson 3 `@JsonNaming`, so `JacksonNamingIntrospector` reads it **per class**, like the real
  mapper. A DTO without it is documented with its Java (camelCase) names, and `DtoNamingTest`
  fails on any `*Dto/*Request/*Response` with a multi-word field and no `@JsonNaming`/`@JsonProperty`.
- `OpenApiContractIT` fails when the API and the committed file differ, and also checks the main
  schemas against the real JSON field names pinned by `ApiContractIT`.
- Controllers return concrete types (`ApiResponse<PagedResponse<LogEntryDto>>`, never
  `ApiResponse<?>`), otherwise the response is published as an untyped object; the IT walks every
  response schema and fails on a shapeless object (one documented exception: the empty OTLP reply).
- **Operation ids** are `<controller><Method>` (`sourceList`, `logGroupEntries`), never numbered.
- **Success status**: handlers that answer 201/202/204 carry `@ResponseStatus`, so the document says
  so (`ResponseStatusTest` enforces it for every `ResponseEntity<Void>` handler).
- **Errors**: every operation has a `default` response, the error envelope
  `{"success": false, "error": {"code", "message", "timestamp"}}` (`ErrorEnvelope`, `ErrorBody`).
- **Credentials** (`securitySchemes`): `bearerAuth` (user JWT) is the default; operations an API-key
  scope covers list `apiKey` (`X-FaultLens-Key` or `Authorization: Bearer flk_…`) with the scope.
  Derived from `ApiKeyScope`; ingest endpoints take an API key only.
- The document is OpenAPI 3.1 throughout (schemas included: nullability as a type array).
- After an intended API change:

  ```bash
  cd faultlens-backend
  ./mvnw verify -DskipUTs -pl api -am -Dit.test=OpenApiContractIT -Dcontracts.update=true
  cd ../faultlens-frontend && pnpm gen:api && pnpm typecheck
  ```

  and commit `contracts/openapi.json` with `shared/api/schema.d.ts`. CI fails if either is stale.

---

## 1. Authentication Endpoints (`/api/v1/auth`)

`/login` and `/refresh` are public; `/profile` and `/password` require a bearer token.

> **Initial admin account.** On first start the backend creates `admin` with the password from
> `ADMIN_INITIAL_PASSWORD`, or generates one and prints it **once** in the backend log. An admin
> still using a well-known default (the old `admin123` / V6 seed) is reset the same way on startup.
> Either way the admin must change the password at first login (see *Password change required*).

### A. Login User
- **Method:** `POST`
- **Path:** `/api/v1/auth/login`
- **Request Body (`application/json`):**
  ```json
  {
    "username": "admin",
    "password": "<password>"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIs...",
      "expires_at": "2026-05-26T12:22:00.089Z",
      "must_change_password": false
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:30:00.000Z"
  }
  ```
- **Error Codes:** `401 AUTH_FAILED` (invalid user/pass).
- **Password change required:** when `must_change_password` is `true`, the token only allows
  `PUT /auth/password`, `GET /auth/profile` and `POST /auth/refresh`. Every other endpoint answers
  `403` with error code `PASSWORD_CHANGE_REQUIRED`, and the WebSocket handshake is refused (`403`).

### B. Refresh Token
- **Method:** `POST`
- **Path:** `/api/v1/auth/refresh`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>`
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIs...",
      "expires_at": "2026-05-27T12:22:00.000Z"
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:31:00.000Z"
  }
  ```

### C. Change Password
- **Method:** `PUT`
- **Path:** `/api/v1/auth/password`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>`
- **Request Body:** `{ "current_password": "...", "new_password": "..." }` (`new_password`: 8-128 characters, must differ from the current one)
- **Response (`200 OK`):** a fresh token (same shape as login) with `must_change_password: false`. Clients should replace their stored token.
- **Error Codes:** `400 INVALID_CURRENT_PASSWORD`, `400 PASSWORD_REUSED`, `422 VALIDATION_FAILED`. A wrong current password is deliberately **not** `401`, so clients do not treat it as a logged-out session.

---

## 1b. API Keys (`/api/v1/api-keys`)

Scoped service tokens for CI and integrations (P10 / 1.1). Management endpoints are **ADMIN only**
(user JWT); an API key can never call them.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/api-keys` | All keys, newest first (masked `display_key`, never the token) |
| `POST` | `/api/v1/api-keys` | `{ name, scopes: [...], expires_in_days? }` → `201 { key, token }` — `token` is shown **only here** |
| `DELETE` | `/api/v1/api-keys/{id}` | Revoke (idempotent, `204`); the row is kept for auditing |

**Token format:** `flk_<8-char prefix>_<32-char secret>`; only its SHA-256 is stored.
**Sending a key:** `Authorization: Bearer flk_…` or `X-FaultLens-Key: flk_…`.

**Scopes** — a key may call *only* these endpoints; anything else is `403 API_KEY_SCOPE`:

| Scope | Allows |
|---|---|
| `deployments:write` | `POST /api/v1/deployments` |
| `logs:ingest` | `POST /api/v1/ingest/**`, `POST /v1/logs` (HTTP / OTLP ingest, see 3b); needs `source_id` (an HTTP source) at creation |
| `logs:read` | `GET /api/v1/logs`, `GET /api/v1/logs/**` |
| `alarms:read` | `GET /api/v1/alarms`, `GET /api/v1/alarms/**` |

**Errors:** unknown / revoked / expired key → `401 INVALID_API_KEY`; outside its scopes → `403 API_KEY_SCOPE`;
over the per-key limit (default 600 requests/minute, `faultlens.api-keys.rate-limit-per-minute`) →
`429 RATE_LIMITED` with `Retry-After`; unknown scope at creation → `400 INVALID_SCOPE`.

---

## 2. Log Source Management (`/api/v1/sources`)

All source endpoints require **ADMIN** or **USER** authentication.

### A. Get All Sources
- **Method:** `GET`
- **Path:** `/api/v1/sources`
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": 1,
        "name": "Local Server Logs",
        "type": "SSH",
        "config": "{\"host\":\"localhost\",\"port\":\"22\",\"username\":\"admin\"}",
        "enabled": true,
        "created_at": "2026-05-25T15:00:00Z",
        "last_seen_at": "2026-05-25T15:35:00Z"
      }
    ],
    "message": "OK",
    "timestamp": "2026-05-25T15:35:00.000Z"
  }
  ```

### B. Create Source
- **Method:** `POST`
- **Path:** `/api/v1/sources`
- **Request Body (`application/json`):**
  ```json
  {
    "name": "Production Docker Engine",
    "type": "DOCKER",
    "config": "{\"dockerHost\":\"tcp://10.0.0.5:2375\"}",
    "enabled": true
  }
  ```
- **Response (`201 Created`):**
  ```json
  {
    "success": true,
    "data": {
      "id": 2,
      "name": "Production Docker Engine",
      "type": "DOCKER",
      "config": "{\"dockerHost\":\"tcp://10.0.0.5:2375\"}",
      "enabled": true,
      "created_at": "2026-05-25T15:40:00Z"
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:40:00.000Z"
  }
  ```

### C. Update Source
- **Method:** `PUT`
- **Path:** `/api/v1/sources/{id}`
- **Request Body (`application/json`):**
  ```json
  {
    "name": "Production Docker Engine Updated",
    "config": "{\"dockerHost\":\"tcp://10.0.0.5:2376\"}",
    "enabled": false
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "id": 2,
      "name": "Production Docker Engine Updated",
      "type": "DOCKER",
      "config": "{\"dockerHost\":\"tcp://10.0.0.5:2376\"}",
      "enabled": false,
      "created_at": "2026-05-25T15:40:00Z"
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:41:00.000Z"
  }
  ```

### D. Delete Source
- **Method:** `DELETE`
- **Path:** `/api/v1/sources/{id}`
- **Response (`204 No Content`):** Empty body.

### E. Test Source Connection
- **Method:** `POST`
- **Path:** `/api/v1/sources/{id}/test`
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "connected": true
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:42:00.000Z"
  }
  ```

---

## 3. Log Ingestion & Query APIs (`/api/v1/logs`)

Requires authentication.

### A. Query Log Entries
- **Method:** `GET`
- **Path:** `/api/v1/logs`
- **Query Parameters:**
  - `page` (int, default: 0)
  - `size` (int, default: 20)
  - `severity` (string list, e.g. `ERROR,CRITICAL`, optional)
  - `startTime` (ISO-8601 string, optional)
  - `endTime` (ISO-8601 string, optional)
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "content": [
        {
          "id": 125,
          "source_id": 1,
          "group_id": 12,
          "severity": "ERROR",
          "message": "java.lang.NullPointerException: Cannot invoke \"String.toLowerCase()\"...",
          "timestamp": "2026-05-25T15:30:00Z",
          "parsed_message": "Cannot invoke \"String.toLowerCase()\"...",
          "stack_trace": "java.lang.NullPointerException: ...",
          "service_name": "auth-service"
        }
      ],
      "page_number": 0,
      "page_size": 20,
      "total_elements": 1,
      "total_pages": 1,
      "last": true
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:42:00.000Z"
  }
  ```

### B. Query Log Groups (Templates)
- **Method:** `GET`
- **Path:** `/api/v1/logs/groups`
- **Query Parameters:**
  - `page` (int, default: 0)
  - `size` (int, default: 10)
  - `severity` (string, e.g. `ERROR`, optional)
- **Response (`200 OK`):** Returns standard `PagedResponse<LogGroupDto>`.

### C. Get Log Entry Detail
- **Method:** `GET`
- **Path:** `/api/v1/logs/{id}`
- **Response (`200 OK`):** Returns `LogDetailDto` containing detailed properties, host/service name, stack trace, and correlation list.

### D. Get Real-time Analytics Statistics
- **Method:** `GET`
- **Path:** `/api/v1/logs/stats`
- **Query Parameters:**
  - `windowMinutes` (int, default: 60)
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "window_minutes": 60,
      "total_count": 1540,
      "severity_counts": {
        "INFO": 1400,
        "WARN": 100,
        "ERROR": 35,
        "CRITICAL": 5
      }
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:45:00.000Z"
  }
  ```
  *(Note: This endpoint is high-performance cached in Redis with a 1-minute expiration).*

---

## 3b. Log Ingest (`/api/v1/ingest/logs`, `/v1/logs`)

Applications push logs themselves (P10 / 1.2). Only API keys with the `logs:ingest` scope may call
these endpoints (user tokens get `403`); the key is bound to an **HTTP** source (`source_id` at key
creation), and every record is stored under that source. Records go to the `log-raw` topic like
collected logs.

| Method | Path | Body | Success |
|---|---|---|---|
| `POST` | `/api/v1/ingest/logs` | JSON array of records, `{"logs": [...]}` or one record | `202 { data: { accepted } }` |
| `POST` | `/v1/logs` | OTLP `ExportLogsServiceRequest`, `application/x-protobuf` or `application/json` | `200` empty `ExportLogsServiceResponse` (same encoding) |

**JSON record** (snake_case; camelCase accepted): `message` (required), `timestamp` (ISO-8601 or epoch
ms), `severity`, `stack_trace`, `service_name`, `namespace`, `pod_name`, `container_name`, `cluster`,
`attributes` (string map, max 32).

**OTLP mapping:** body → message; `exception.stacktrace` → stack trace; `service.name`,
`k8s.namespace.name`, `k8s.pod.name`, `k8s.container.name` / `container.name`, `k8s.cluster.name`
(resource or record attributes) → fields; `severity_number` ranges → severity, else `severity_text`.

**All or nothing:** a request is validated as a whole and answered after Kafka acknowledged every record.

| Error | When |
|---|---|
| `400 INVALID_JSON` / `INVALID_OTLP` / `INVALID_LOG_RECORD` | malformed body, a record without message, bad timestamp |
| `403 API_KEY_NO_SOURCE` | key not bound to an existing HTTP source |
| `409 SOURCE_DISABLED` | the source is disabled |
| `413 TOO_MANY_RECORDS` / `PAYLOAD_TOO_LARGE` | > `faultlens.ingest.max-records` (5000) / > `faultlens.ingest.max-body-bytes` (5 MB) |
| `503 INGEST_UNAVAILABLE` + `Retry-After` | Kafka did not acknowledge within `faultlens.ingest.kafka-timeout-ms` (5 s); retry |

Metric: `faultlens_ingest_records_total{source, format=json|otlp, result=accepted|rejected}`.

---

## 4. Anomaly Analysis & Deployments (`/api/v1/analyses`, `/api/v1/deployments`)

Requires authentication.

### A. Get Anomaly Analyses for Log Entry
- **Method:** `GET`
- **Path:** `/api/v1/analyses`
- **Query Parameters:**
  - `logEntryId` (Long, required)
- **Response (`200 OK`):** Returns standard list of analysis reports.

### B. Create Deployment Event
- **Method:** `POST`
- **Path:** `/api/v1/deployments`
- **Auth:** EDITOR/ADMIN token, or an API key with the `deployments:write` scope (see 1b)
- **Request Body (`application/json`):**
  ```json
  {
    "serviceName": "auth-service",
    "version": "v1.1.0",
    "deployedAt": "2026-05-25T15:20:00Z",
    "notes": "Enabled performance optimizations and standard security updates."
  }
  ```
- **Response (`200 OK`):** Returns the saved `DeploymentDto`.

### C. List Deployments
- **Method:** `GET`
- **Path:** `/api/v1/deployments?q=&environment=&page=0&size=50`
- **Query Parameters:** `q` part of the service name (case-insensitive; `%` and `_` are literal),
  `environment` exact match, `page` (from 0), `size` (1-200, default 50). Filtering and paging run in
  the database, newest first.
- **Response (`200 OK`):** a page (`content`, `page`, `size`, `total_elements`, `total_pages`) of
  `DeploymentDto` (`id, service_name, version, environment, deployed_at, deployed_by, status, notes`).

---

## 4b. Alarms (`/api/v1/alarms`)

All endpoints require a bearer token and use the standard `ApiResponse` wrapper; fields are snake_case.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/alarms?status=&severity=&page=0&size=20` | Paged list, newest first. `status`: `ACTIVE` / `ACKNOWLEDGED` / `RESOLVED`; `size` 1-200 |
| `GET` | `/api/v1/alarms/stats` | `{ active, acknowledged, resolved, open_critical }` (independent of paging) |
| `PUT` | `/api/v1/alarms/{id}/ack` | `ACTIVE` -> `ACKNOWLEDGED`; `400 ALARM_ALREADY_RESOLVED` for resolved alarms |
| `PUT` | `/api/v1/alarms/{id}/resolve` | -> `RESOLVED` (idempotent, keeps the first `resolved_at`) |
| `DELETE` | `/api/v1/alarms/{id}` | `204`; `404` when the alarm does not exist |

Alarm object: `id, rule_id, rule_name, source_id, severity, status, message, triggered_at, resolved_at`.

## 5. Real-time Log Stream WebSocket (`/ws/logs`)

A stateless, full-duplex WebSocket stream intended for front-end dashboards to receive parsed, structured logs instantly.

- **Endpoint:** `ws://localhost:3500/ws/logs`
- **Authentication (required):** send the JWT from `/api/v1/auth/login` as WebSocket sub-protocols `["bearer", <token>]`. Browsers cannot set an `Authorization` header on WebSocket requests, and a query parameter would leak the token into proxy/access logs.
  ```js
  const ws = new WebSocket("ws://localhost:3500/ws/logs", ["bearer", token]);
  // server selects the "bearer" sub-protocol: ws.protocol === "bearer"
  ```
  CLI example: `wscat -c ws://localhost:3500/ws/logs -s bearer -s <token> -o http://localhost:3000`
- **Handshake rejections:**
  | Status | Reason |
  |---|---|
  | `401` | Missing, malformed, wrongly signed or expired token |
  | `403` | `Origin` not in `CORS_ORIGINS` (same allow-list as the REST API) |
  | `503` | Global connection limit reached (`faultlens.ws.max-connections`, default 1000) |
- **Token expiry:** the server checks open sessions every 10 seconds and closes those whose token has expired with close code **`4401` (`Token expired`)**. Clients should not auto-retry on `4401`; reconnect after obtaining a new token.
- **Message Payload Broadcast (Server to Client):**
  ```json
  {
    "id": "e458e0a3-f033-4f9e-a0e2-882ab6182c12",
    "source": "ssh",
    "cluster": "127.0.0.1",
    "service_name": "Lokal Sunucu",
    "severity": "INFO",
    "message": "[2026-05-25 15:00:00] [INFO] Request completed in 45ms.",
    "raw_line": "[2026-05-25 15:00:00] [INFO] Request completed in 45ms.",
    "timestamp": "2026-05-25T15:00:00Z",
    "labels": {
      "sourceId": "1"
    }
  }
  ```

---

## 6. Swagger JWT Authorization Guideline

To test protected endpoints using the Swagger UI:
1. Log in via `POST /api/v1/auth/login` to obtain the bearer JWT token.
2. Copy the token value from the response `data.token`.
3. In Swagger UI (`http://localhost:3500/swagger-ui/index.html`), click the **Authorize (padlock)** button in the top-right corner.
4. Paste the copied token value and click **Authorize**.
5. You can now execute and test all protected endpoints seamlessly.
