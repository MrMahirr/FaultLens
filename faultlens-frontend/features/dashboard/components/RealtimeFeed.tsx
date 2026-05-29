"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Radio } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { useLogs } from "@/features/logs/api/logs.queries";
import { useLogStream } from "@/features/logs/hooks/useLogStream";
import type { LogEntryDto } from "@/features/logs/types/log.types";
import { Severity } from "@/shared/types/common.types";
import { formatRelativeTime, truncate } from "@/shared/lib/utils";
import { cn } from "@/shared/lib/utils";

/* ── Severity Badge Variant Map ────────────────────────────── */

function getSeverityVariant(severity: Severity) {
  switch (severity) {
    case Severity.CRITICAL:
      return "critical" as const;
    case Severity.ERROR:
      return "error" as const;
    case Severity.WARN:
      return "warning" as const;
    case Severity.INFO:
      return "info" as const;
    case Severity.DEBUG:
      return "accent" as const;
    case Severity.TRACE:
      return "default" as const;
  }
}

/* ── Component ─────────────────────────────────────────────── */

function RealtimeFeed() {
  const [logs, setLogs] = useState<LogEntryDto[]>([]);

  // Fetch initial error/critical logs
  const { data: initialData } = useLogs({
    page: 0,
    size: 20,
    severity: [Severity.ERROR, Severity.CRITICAL],
  });

  // Sync initial logs when query completes
  useEffect(() => {
    if (initialData?.content) {
      setLogs(initialData.content);
    }
  }, [initialData]);

  // Connect to live WebSocket log stream
  const { isConnected } = useLogStream({
    enabled: true,
    onNewLog: (newLog) => {
      if (newLog.severity === Severity.ERROR || newLog.severity === Severity.CRITICAL) {
        setLogs((prev) => {
          // Avoid duplicate logs in list if query also fetched them
          if (prev.some((l) => l.id === newLog.id)) return prev;
          return [newLog, ...prev].slice(0, 30); // Cap at 30 to prevent memory leaks (OOM)
        });
      }
    },
  });

  return (
    <Card variant="default" padding="none">
      {/* Header */}
      <div className="flex items-center justify-between px-5 pt-5 pb-3">
        <div>
          <h3 className="text-sm font-semibold font-display text-text-primary">
            Gerçek Zamanlı Feed
          </h3>
          <p className="text-xs text-text-muted mt-0.5">
            Son ERROR / CRITICAL loglar
          </p>
        </div>
        <div className="flex items-center gap-1.5">
          <Radio
            size={12}
            className={cn(
              isConnected ? "text-success animate-pulse" : "text-text-muted"
            )}
          />
          <span
            className={cn(
              "text-xs font-medium",
              isConnected ? "text-success" : "text-text-muted"
            )}
          >
            {isConnected ? "Canlı" : "Bağlantı Yok"}
          </span>
        </div>
      </div>

      {/* Feed List */}
      <div className="max-h-[360px] overflow-y-auto">
        <AnimatePresence initial={false}>
          {logs.map((log, index) => (
            <motion.div
              key={log.id || `live-${index}-${log.timestamp}`}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.2 }}
              className={cn(
                "flex items-start gap-3 px-5 py-3",
                "border-b border-border-default",
                "hover:bg-bg-tertiary/50 transition-colors cursor-pointer",
                log.severity === Severity.CRITICAL &&
                  "border-l-2 border-l-critical"
              )}
            >
              <Badge
                variant={getSeverityVariant(log.severity)}
                size="sm"
                pulse={log.severity === Severity.CRITICAL}
              >
                {log.severity}
              </Badge>

              <div className="flex-1 min-w-0">
                <p className="text-sm text-text-primary truncate">
                  {truncate(log.message, 60)}
                </p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs text-text-muted font-mono">
                    {log.podName ?? log.serviceName ?? `Source #${log.sourceId}`}
                  </span>
                  <span className="text-xs text-text-muted">
                    {formatRelativeTime(log.timestamp)}
                  </span>
                </div>
              </div>
            </motion.div>
          ))}
          {logs.length === 0 && (
            <div className="py-8 text-center text-sm text-text-muted">
              Henüz log kaydı yok
            </div>
          )}
        </AnimatePresence>
      </div>
    </Card>
  );
}

export { RealtimeFeed };
