"use client";

import { Badge, type BadgeVariant } from "@/shared/components/ui/Badge";
import { Severity } from "@/shared/types/common.types";

/* ── Props ─────────────────────────────────────────────────── */

interface SeverityBadgeProps {
  severity: Severity;
  className?: string;
}

/* ── Severity → Badge Variant Mapping ──────────────────────── */

function getVariant(severity: Severity): BadgeVariant {
  switch (severity) {
    case Severity.CRITICAL:
      return "critical";
    case Severity.ERROR:
      return "error";
    case Severity.WARN:
      return "warning";
    case Severity.INFO:
      return "info";
    case Severity.DEBUG:
      return "accent";
    case Severity.TRACE:
      return "default";
  }
}

/* ── Component ─────────────────────────────────────────────── */

function SeverityBadge({ severity, className }: SeverityBadgeProps) {
  return (
    <Badge
      variant={getVariant(severity)}
      size="sm"
      pulse={severity === Severity.CRITICAL}
      className={className}
    >
      {severity}
    </Badge>
  );
}

export { SeverityBadge, type SeverityBadgeProps };
