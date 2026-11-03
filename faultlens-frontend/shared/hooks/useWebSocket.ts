"use client";

import { useRef, useEffect, useCallback, useState } from "react";

/* ── Types ─────────────────────────────────────────────────── */

type WebSocketStatus = "connecting" | "connected" | "disconnected";

/** Sub-protocol marker the backend expects before the JWT (see JwtHandshakeInterceptor). */
const BEARER_PROTOCOL = "bearer";
/** Close code the backend sends when the session's token has expired. */
const TOKEN_EXPIRED_CLOSE_CODE = 4401;

interface UseWebSocketOptions {
  url: string;
  /**
   * JWT sent as the `["bearer", token]` sub-protocol. Browsers cannot set an
   * Authorization header on WebSocket requests, and a query param would leak
   * the token into proxy/access logs.
   */
  token?: string | null;
  onMessage?: (data: unknown) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: Event) => void;
  enabled?: boolean;
  maxRetries?: number;
  baseDelay?: number;
  maxDelay?: number;
}

interface UseWebSocketReturn {
  status: WebSocketStatus;
  send: (data: string | object) => void;
  disconnect: () => void;
  reconnect: () => void;
}

/* ── Hook ──────────────────────────────────────────────────── */

export function useWebSocket({
  url,
  token,
  onMessage,
  onOpen,
  onClose,
  onError,
  enabled = true,
  maxRetries = 10,
  baseDelay = 1000,
  maxDelay = 30000,
}: UseWebSocketOptions): UseWebSocketReturn {
  const [status, setStatus] = useState<WebSocketStatus>("disconnected");
  const wsRef = useRef<WebSocket | null>(null);
  const retryCountRef = useRef(0);
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const enabledRef = useRef(enabled);
  const mountedRef = useRef(true);
  // Reconnect timers call the latest `connect` through this ref (a callback
  // cannot reference itself while it is being declared).
  const connectRef = useRef<() => void>(() => {});

  // Refs for callbacks to prevent unnecessary reconnects
  const onMessageRef = useRef(onMessage);
  const onOpenRef = useRef(onOpen);
  const onCloseRef = useRef(onClose);
  const onErrorRef = useRef(onError);

  // Keep refs updated
  useEffect(() => {
    onMessageRef.current = onMessage;
    onOpenRef.current = onOpen;
    onCloseRef.current = onClose;
    onErrorRef.current = onError;
    enabledRef.current = enabled;
  }, [onMessage, onOpen, onClose, onError, enabled]);

  const connect = useCallback(() => {
    if (!enabledRef.current || !mountedRef.current) return;

    // Clean up previous connection
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    setStatus("connecting");

    try {
      const ws = token
        ? new WebSocket(url, [BEARER_PROTOCOL, token])
        : new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        if (!mountedRef.current) return;
        setStatus("connected");
        retryCountRef.current = 0;
        onOpenRef.current?.();
      };

      ws.onmessage = (event) => {
        if (!mountedRef.current) return;
        try {
          const parsed = JSON.parse(event.data);
          onMessageRef.current?.(parsed);
        } catch {
          onMessageRef.current?.(event.data);
        }
      };

      ws.onerror = (event) => {
        if (!mountedRef.current) return;
        onErrorRef.current?.(event);
      };

      ws.onclose = (event) => {
        if (!mountedRef.current) return;
        setStatus("disconnected");
        onCloseRef.current?.();

        // An expired token will not become valid by retrying; a new token
        // (after re-login) changes `token` and triggers a fresh connect.
        if (event.code === TOKEN_EXPIRED_CLOSE_CODE) return;

        // Auto-reconnect with exponential backoff
        if (
          enabledRef.current &&
          retryCountRef.current < maxRetries &&
          mountedRef.current
        ) {
          const delay = Math.min(
            baseDelay * Math.pow(2, retryCountRef.current),
            maxDelay,
          );
          retryCountRef.current += 1;
          retryTimerRef.current = setTimeout(() => connectRef.current(), delay);
        }
      };
    } catch {
      if (mountedRef.current) {
        setStatus("disconnected");
      }
    }
  }, [url, token, maxRetries, baseDelay, maxDelay]);

  useEffect(() => {
    connectRef.current = connect;
  }, [connect]);

  const disconnect = useCallback(() => {
    if (retryTimerRef.current) {
      clearTimeout(retryTimerRef.current);
    }
    retryCountRef.current = maxRetries; // Prevent reconnection
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setStatus("disconnected");
  }, [maxRetries]);

  const reconnect = useCallback(() => {
    retryCountRef.current = 0;
    connect();
  }, [connect]);

  const send = useCallback((data: string | object) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      const message = typeof data === "string" ? data : JSON.stringify(data);
      wsRef.current.send(message);
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;

    if (enabled) {
      connect();
    }

    return () => {
      mountedRef.current = false;
      if (retryTimerRef.current) {
        clearTimeout(retryTimerRef.current);
      }
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [enabled, connect]);

  return { status, send, disconnect, reconnect };
}
