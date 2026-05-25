"use client";

import { motion, AnimatePresence } from "framer-motion";
import { Radio } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { mockLogEntries } from "@/shared/mocks/data";
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
  // Filter only ERROR and CRITICAL logs for the realtime feed
  const realtimeLogs = mockLogEntries
    .filter(
      (log) =>
        log.severity === Severity.ERROR || log.severity === Severity.CRITICAL
    )
    .slice(0, 20);

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
          <Radio size={12} className="text-success animate-pulse" />
          <span className="text-xs text-success font-medium">Canlı</span>
        </div>
      </div>

      {/* Feed List */}
      <div className="max-h-[360px] overflow-y-auto">
        <AnimatePresence initial={false}>
          {realtimeLogs.map((log, index) => (
            <motion.div
              key={log.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.05, duration: 0.3 }}
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
                    {log.podName ?? log.source}
                  </span>
                  <span className="text-xs text-text-muted">
                    {formatRelativeTime(log.timestamp)}
                  </span>
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </Card>
  );
}

export { RealtimeFeed };
