/** Backend ApiKeyScope values (P10 / 1.1); shown untranslated like other API words. */
export const API_KEY_SCOPES = [
  "deployments:write",
  "logs:ingest",
  "logs:read",
  "alarms:read",
] as const;
export type ApiKeyScope = (typeof API_KEY_SCOPES)[number];

/** Backend ApiKeyResponse (camelCase via the axios interceptor). */
export interface ApiKey {
  id: number;
  name: string;
  /** Masked, e.g. "flk_Ab12Cd34_…" — the full token is never returned again. */
  displayKey: string;
  scopes: ApiKeyScope[];
  /** HTTP source of a logs:ingest key. */
  sourceId: number | null;
  createdBy: string | null;
  createdAt: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
  lastUsedIp: string | null;
  revokedAt: string | null;
  active: boolean;
}

export interface CreateApiKeyInput {
  name: string;
  scopes: ApiKeyScope[];
  /** Omitted = never expires. */
  expiresInDays?: number;
  /** Required with logs:ingest: the HTTP source the pushed logs belong to. */
  sourceId?: number;
}

/** Creation result: `token` is the only time the full key is visible. */
export interface CreatedApiKey {
  key: ApiKey;
  token: string;
}
