# FaultLens Backend

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://jdk.java.net/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.2.0-orange.svg)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-10.13.0-red.svg)](https://flywaydb.org/)

FaultLens (also known as LogLens) is a modern, high-throughput distributed log parser, templated indexer, and real-time anomaly analysis platform. This repository contains the multi-module Maven backend application.

---

## 📖 Table of Contents
1. [Core Modules](#1-core-modules)
2. [Prerequisites](#2-prerequisites)
3. [Local Development Setup](#3-local-development-setup)
4. [Quick Integration & Smoke Test](#4-quick-integration--smoke-test)
5. [Architecture & API Spec Details](#5-architecture--api-spec-details)
6. [Troubleshooting & Ports](#6-troubleshooting--ports)

---

## 1. Core Modules

The application is engineered on top of SOLID and Clean Architecture principles split across 5 dedicated modules:
- **`common`**: Core domain contracts, DTO mappings, standard exceptions, and shared enums.
- **`collector`**: Log source adapters (SSH, Docker, Kubernetes) streaming events into Kafka.
- **`processor`**: Pattern matching (Java, Spring, Nginx) registry, SHA-256 fingerprinting, and PostgreSQL ingestion.
- **`analyzer`**: Rule-based exception matching (NPE, OOM, StackOverflow) and OpenAI AI engine fallback.
- **`api`**: REST services, JWT security layer, Actuator metrics, Redis caching, and real-time WebSockets.

---

## 2. Prerequisites

Ensure you have the following installed locally:
- **Java Development Kit (JDK) 21**
- **Apache Maven 3.9+** (or use the provided `.\mvnw.cmd` / `./mvnw` wrappers)
- **Docker & Docker Compose**

---

## 3. Local Development Setup

### Step 1: Initialize Docker Infrastructure
Start PostgreSQL, Redis, Kafka, ZooKeeper, and Kafka UI in the background:
```bash
docker-compose up -d
```

### Step 2: Build & Package Backend Modules
Run clean compilation and module assembly packaging:
```bash
.\mvnw.cmd clean package -DskipTests
```

### Step 3: Run the API Executable Application
The backend port defaults to `3500` (synced with frontend `.env.local` configuration):
```bash
java -jar api/target/api-1.0.0-SNAPSHOT.jar
```

---

## 4. Quick Integration & Smoke Test

Once the application starts, you can verify basic services using the following curl commands.

### A. Health & Metrics Check (Actuator)
- **Public Health Endpoint:**
  ```bash
  curl -i http://localhost:3500/actuator/health
  ```
  *Response:* `200 OK` $\rightarrow$ `{"status":"UP"}`
  
- **Secure Metrics Endpoint:**
  ```bash
  curl -i http://localhost:3500/actuator/metrics
  ```
  *Response:* `401 Unauthorized` (Protected for ADMIN).

### B. User Authentication (Login)
Retrieve a stateless JWT token using the seeded seed admin:
```bash
curl -i -X POST http://localhost:3500/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

### C. Swagger UI Dashboard
Access the fully functional interactive API reference dashboard:
- URL: **[http://localhost:3500/swagger-ui/index.html](http://localhost:3500/swagger-ui/index.html)**
- Click the **Authorize** lock button in the top-right and paste your JWT token to test secure APIs directly.

---

## 5. Architecture & API Spec Details

- For advanced architectural designs, data flows, thread allocations, and locking details, see **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.
- For complete endpoint tables, payloads, schemas, WebSockets, and status codes, see **[docs/API.md](docs/API.md)**.

---

## 6. Troubleshooting & Ports

### Port Reference Mapping
| Service | Host Port | Internal Container Port |
| :--- | :--- | :--- |
| **FaultLens Backend** | `3500` | - |
| **PostgreSQL** | `5433` | `5432` |
| **Redis** | `6379` | `6379` |
| **Kafka Broker** | `9092` | `9092` |
| **Zookeeper** | `2181` | `2181` |
| **Kafka UI Dashboard** | `8090` | `8080` |

### Common Issues
1. **Flyway Checksum Mismatch:**
   If you have stale local migration tables, stop Docker containers, clear volumes, and spin them up fresh:
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```
2. **Local PostgreSQL Conflicts:**
   If a native PostgreSQL service is running locally on port `5432`, the docker PostgreSQL maps automatically to port `5433`. The backend is configured to dynamically check `localhost:5433` as a fallback.
