/**
 * Backend API endpoint definitions.
 * Hiçbir feature dosyasında URL string'i olmayacak — sadece Endpoints.* kullanılır.
 */
// NEXT_PUBLIC_* values are inlined at build time. `||` (not `??`) so an empty
// value — e.g. a Docker build ARG that was not passed — falls back to the default.
const BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:3500/api/v1";
const WS = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:3500/ws";

export const Endpoints = {
  // ── Auth ──────────────────────────────────────────────────
  AUTH: {
    LOGIN: `${BASE}/auth/login`,
    REFRESH: `${BASE}/auth/refresh`,
    PROFILE: `${BASE}/auth/profile`,
    CHANGE_PASSWORD: `${BASE}/auth/password`,
  },

  // ── Logs ──────────────────────────────────────────────────
  LOGS: {
    LIST: `${BASE}/logs`,
    DETAIL: (id: number) => `${BASE}/logs/${id}`,
    GROUPS: `${BASE}/logs/groups`,
    GROUP_ENTRIES: (groupId: number) =>
      `${BASE}/logs/groups/${groupId}/entries`,
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
    },
  },

  // ── WebSocket ─────────────────────────────────────────────
  WS: {
    LOGS: `${WS}/logs`,
  },
} as const;
