import { setupServer } from "msw/node";

/** Shared MSW server; each test registers its own handlers with `server.use(...)`. */
export const server = setupServer();

export const API = "http://localhost:3500/api/v1";

/** Backend success envelope (ApiResponse), snake_case like the real API. */
export function ok<T>(data: T) {
  return { success: true, data, message: "OK", timestamp: "2026-11-04T10:00:00Z" };
}

/** Backend error envelope (ErrorEnvelope). */
export function fail(code: string, message: string) {
  return { success: false, error: { code, message, timestamp: "2026-11-04T10:00:00Z" } };
}
