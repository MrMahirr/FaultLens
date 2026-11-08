/**
 * Backend API endpoint definitions.
 * Hiçbir feature dosyasında URL string'i olmayacak — sadece Endpoints.* kullanılır.
 */
// NEXT_PUBLIC_* values are inlined at build time. `||` (not `??`) so an empty
// value — e.g. a Docker build ARG that was not passed — falls back to the default.
const BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:3500/api/v1";
const WS = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:3500/ws";
/** The API base without its /api/v1 suffix (OTLP is served at the root: /v1/logs). */
const API_ORIGIN = BASE.replace(/\/api\/v1\/?$/, "");

export const Endpoints = {
  // ── Auth ──────────────────────────────────────────────────
  AUTH: {
    LOGIN: `${BASE}/auth/login`,
    REFRESH: `${BASE}/auth/refresh`,
    LOGOUT: `${BASE}/auth/logout`,
    PROFILE: `${BASE}/auth/profile`,
    CHANGE_PASSWORD: `${BASE}/auth/password`,
  },

  // ── Logs ──────────────────────────────────────────────────
  LOGS: {
    LIST: `${BASE}/logs`,
    DETAIL: (id: number) => `${BASE}/logs/${id}`,
    GROUPS: `${BASE}/logs/groups`,
    GROUP: (groupId: number) => `${BASE}/logs/groups/${groupId}`,
    GROUP_ENTRIES: (groupId: number) => `${BASE}/logs/groups/${groupId}/entries`,
    GROUP_EVENTS: (groupId: number) => `${BASE}/logs/groups/${groupId}/events`,
    STATS: `${BASE}/logs/stats`,
  },

  // ── Sources ───────────────────────────────────────────────
  SOURCES: {
    LIST: `${BASE}/sources`,
    DETAIL: (id: number) => `${BASE}/sources/${id}`,
    CREATE: `${BASE}/sources`,
    TEST: (id: number) => `${BASE}/sources/${id}/test`,
    TEST_CONFIG: `${BASE}/sources/test-config`,
    ENABLE: (id: number) => `${BASE}/sources/${id}/enable`,
    DISABLE: (id: number) => `${BASE}/sources/${id}/disable`,
  },

  // ── Alarms ────────────────────────────────────────────────
  ALARMS: {
    LIST: `${BASE}/alarms`,
    STATS: `${BASE}/alarms/stats`,
    ACK: (id: number) => `${BASE}/alarms/${id}/ack`,
    RESOLVE: (id: number) => `${BASE}/alarms/${id}/resolve`,
    DELETE: (id: number) => `${BASE}/alarms/${id}`,
    RULES: `${BASE}/alarms/rules`,
    RULE: (id: number) => `${BASE}/alarms/rules/${id}`,
    RULE_ENABLE: (id: number) => `${BASE}/alarms/rules/${id}/enable`,
    RULE_DISABLE: (id: number) => `${BASE}/alarms/rules/${id}/disable`,
  },

  // ── Users (ADMIN) ─────────────────────────────────────────
  USERS: {
    LIST: `${BASE}/users`,
    DETAIL: (id: number) => `${BASE}/users/${id}`,
    ROLE: (id: number) => `${BASE}/users/${id}/role`,
    RESET_PASSWORD: (id: number) => `${BASE}/users/${id}/reset-password`,
  },

  // ── Analyses ──────────────────────────────────────────────
  ANALYSES: {
    LIST: `${BASE}/analyses`,
    DETAIL: (id: number) => `${BASE}/analyses/${id}`,
    TRIGGER: (groupId: number) => `${BASE}/analyses/trigger/${groupId}`,
    DELETE_HISTORY: `${BASE}/analyses`,
  },

  // ── Deployments ───────────────────────────────────────────
  DEPLOYMENTS: {
    LIST: `${BASE}/deployments`,
    CREATE: `${BASE}/deployments`,
  },

  // ── Settings ──────────────────────────────────────────────
  SETTINGS: {
    NOTIFICATIONS: `${BASE}/settings/notifications`,
    SYSTEM: `${BASE}/settings/system`,
    TEST: {
      SLACK: `${BASE}/settings/notifications/test/slack`,
      EMAIL: `${BASE}/settings/notifications/test/email`,
      PUSH: `${BASE}/settings/notifications/test/push`,
      WEBHOOK: `${BASE}/settings/notifications/test/webhook`,
    },
  },

  // ── Ingest (P10 / 1.2, called by applications with a logs:ingest key) ──
  INGEST: {
    JSON: `${BASE}/ingest/logs`,
    /** OTLP/HTTP base: the OpenTelemetry Collector appends /v1/logs itself. */
    OTLP_BASE: API_ORIGIN,
    OTLP_LOGS: `${API_ORIGIN}/v1/logs`,
  },

  // ── API keys (P10 / 1.1, ADMIN only) ──────────────────────
  API_KEYS: {
    LIST: `${BASE}/api-keys`,
    DETAIL: (id: number) => `${BASE}/api-keys/${id}`,
  },

  // ── WebSocket ─────────────────────────────────────────────
  WS: {
    LOGS: `${WS}/logs`,
  },
} as const;
