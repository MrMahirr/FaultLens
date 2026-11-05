"use client";

import { Endpoints } from "@/shared/api/endpoints";
import { useWebSocket } from "@/shared/hooks/useWebSocket";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { LogEntryDto } from "@/features/logs/types/log.types";

/**
 * WebSocket log stream hook.
 * Connects to the log WebSocket endpoint and parses incoming messages.
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
      // Parse incoming log entry
      const log = data as LogEntryDto;
      onNewLog?.(log);
    },
  });

  return {
    status,
    disconnect,
    reconnect,
    isConnected: status === "connected",
  };
}
