# FaultLens API Contract Specification

This document details the REST API endpoints and real-time WebSocket channels provided by the FaultLens (LogLens) backend application.

- **Base URL:** `http://localhost:3500/api/v1`
- **WebSocket Protocol URL:** `ws://localhost:3500/ws`
- **Swagger Documentation Page:** `http://localhost:3500/swagger-ui/index.html`

---

## 1. Authentication Endpoints (`/api/v1/auth`)

All auth endpoints are public.

### A. Login User
- **Method:** `POST`
- **Path:** `/api/v1/auth/login`
- **Request Body (`application/json`):**
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIs...",
      "expires_at": "2026-05-26T12:22:00.089Z"
    },
    "message": "OK",
    "timestamp": "2026-05-25T15:30:00.000Z"
  }
  ```
- **Error Codes:** `401 BadCredentialsException` (invalid user/pass).

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
- **Request Body (`application/json`):**
  ```json
  {
    "serviceName": "auth-service",
    "version": "v1.1.0",
    "deployedAt": "2026-05-25T15:20:00Z",
    "notes": "Enabled performance optimizations and standard security updates."
  }
  ```
- **Response (`201 Created`):** Returns the saved `DeploymentDto`.

---

## 5. Real-time Log Stream WebSocket (`/ws/logs`)

A stateless, full-duplex WebSocket stream intended for front-end dashboards to receive parsed, structured logs instantly.

- **Endpoint:** `ws://localhost:3500/ws/logs`
- **Connection Handshake:** Publicly accessible, but capped to a maximum of **2 active global concurrent socket sessions** to protect backend resources from OOM.
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
