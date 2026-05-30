/**
 * Backend API endpoint definitions.
 * Hiçbir feature dosyasında URL string'i olmayacak — sadece Endpoints.* kullanılır.
 */
const BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
const WS = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws";

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

  // ── Analyses ──────────────────────────────────────────────
  ANALYSES: {
    LIST: `${BASE}/analyses`,
    DETAIL: (id: number) => `${BASE}/analyses/${id}`,
    TRIGGER: (groupId: number) => `${BASE}/analyses/trigger/${groupId}`,
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
  },

  // ── WebSocket ─────────────────────────────────────────────
  WS: {
    LOGS: `${WS}/logs`,
  },
} as const;
