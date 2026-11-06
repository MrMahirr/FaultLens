"use client";

import { cn } from "@/shared/lib/utils";
import { Severity } from "@/shared/types/common.types";

/** Severity → Instrument colour token class (the colours themselves live in globals.css). */
const SEVERITY_TONE: Record<Severity, string> = {
  [Severity.TRACE]: "sev-trace",
  [Severity.DEBUG]: "sev-debug",
  [Severity.INFO]: "sev-info",
  [Severity.WARN]: "sev-warn",
  [Severity.ERROR]: "sev-error",
  [Severity.CRITICAL]: "sev-crit",
};

interface SeverityBadgeProps {
  severity: Severity;
  /** Appends "+" ("ERROR and above"), used by alarm rules. */
  orAbove?: boolean;
  className?: string;
}

/**
 * Severity as colour + dot + text (never colour alone). The enum value itself is shown
 * untranslated, like every API word.
 */
function SeverityBadge({ severity, orAbove = false, className }: SeverityBadgeProps) {
  return (
    <span className={cn("badge mono", SEVERITY_TONE[severity] ?? "sev-info", className)}>
      <span className="d" aria-hidden="true" />
      {severity}
      {orAbove && "+"}
    </span>
  );
}

export { SeverityBadge, SEVERITY_TONE, type SeverityBadgeProps };
