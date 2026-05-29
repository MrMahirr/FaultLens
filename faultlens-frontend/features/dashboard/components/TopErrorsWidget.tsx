"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { TrendingUp } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { mockLogGroups } from "@/shared/mocks/data";
import { Severity } from "@/shared/types/common.types";
import { formatRelativeTime, formatNumber } from "@/shared/lib/utils";

/* ── Component ─────────────────────────────────────────────── */

function TopErrorsWidget() {
  const topGroups = mockLogGroups.slice(0, 5);

  return (
    <Card variant="default" padding="none">
      <div className="flex items-center justify-between px-5 pt-5 pb-3">
        <div>
          <h3 className="text-sm font-semibold font-display text-text-primary">
            En Çok Tekrarlayan Hatalar
          </h3>
          <p className="text-xs text-text-muted mt-0.5">Gruplanmış hatalar</p>
        </div>
        <TrendingUp size={16} className="text-text-muted" />
      </div>

      <div>
        {topGroups.map((group, index) => (
          <motion.div
            key={group.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.08, duration: 0.3 }}
          >
            <Link
              href={`/logs?group=${group.id}`}
              className="flex items-start gap-3 px-5 py-3 border-b border-border-default hover:bg-bg-tertiary/50 transition-colors"
            >
              <Badge
                variant={
                  group.severity === Severity.CRITICAL
                    ? "critical"
                    : group.severity === Severity.ERROR
                      ? "error"
                      : "warning"
                }
                size="sm"
              >
                {formatNumber(group.count)}
              </Badge>

              <div className="flex-1 min-w-0">
                <p className="text-sm text-text-primary truncate">
                  {group.firstMessage}
                </p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs text-text-muted font-mono truncate max-w-[120px]">
                    {group.fingerprint}
                  </span>
                  <span className="text-xs text-text-muted">
                    Son: {formatRelativeTime(group.lastSeenAt)}
                  </span>
                </div>
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
    </Card>
  );
}

export { TopErrorsWidget };
