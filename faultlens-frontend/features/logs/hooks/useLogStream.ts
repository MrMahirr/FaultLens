"use client";

import { Endpoints } from "@/shared/api/endpoints";
import { useWebSocket } from "@/shared/hooks/useWebSocket";
import { snakeToCamel } from "@/shared/lib/caseTransform";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { LogEntryDto } from "@/features/logs/types/log.types";

/** The socket also carries in-app alarms ({ type: "ALARM" }); only log entries pass. */
function toLogEntry(raw: unknown): LogEntryDto | null {
  if (!raw || typeof raw !== "object" || "type" in raw) return null;
  const entry = snakeToCamel<LogEntryDto>(raw);
  return typeof entry.id === "number" && typeof entry.severity === "string" ? entry : null;
}

/**
 * Live log entries from /ws/logs (the backend publishes ERROR/CRITICAL entries), converted
 * to the same camelCase shape as the REST API.
 */
export function useLogStream({
  enabled = false,
  onNewLog,
}: {
  enabled?: boolean;
  onNewLog?: (log: LogEntryDto) => void;
}) {
  const token = useAuthStore((state) => state.token);

  const { status, disconnect, reconnect } = useWebSocket({
    url: Endpoints.WS.LOGS,
    token,
    enabled: enabled && !!token,
    onMessage: (data) => {
      const log = toLogEntry(data);
      if (log) onNewLog?.(log);
    },
  });

  return {
    status,
    disconnect,
    reconnect,
    isConnected: status === "connected",
  };
}
