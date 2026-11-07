"use client";

import { type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

type StatTone = "default" | "ok" | "warn" | "crit" | "info" | "ai" | "signal";

const TONE: Record<StatTone, string> = {
  default: "text-text-muted",
  ok: "text-success",
  warn: "text-warning",
  crit: "text-critical",
  info: "text-info",
  ai: "text-ai",
  signal: "text-accent",
};

interface StatCardProps {
  label: string;
  value: ReactNode;
  icon?: ReactNode;
  tone?: StatTone;
  /** Secondary line under the number (delta, unit, context). */
  hint?: ReactNode;
  loading?: boolean;
  className?: string;
}

/** KPI tile (Instrument .stat): eyebrow label, tabular number, optional hint. */
function StatCard({
  label,
  value,
  icon,
  tone = "default",
  hint,
  loading,
  className,
}: StatCardProps) {
  return (
    <div className={cn("card stat p-4 flex flex-col gap-3 min-w-0", className)}>
      <div className="flex items-center justify-between gap-2">
        <span className="eyebrow truncate">{label}</span>
        {icon && (
          <span className={cn("inline-flex shrink-0", TONE[tone])} aria-hidden="true">
            {icon}
          </span>
        )}
      </div>
      {loading ? (
        <span className="skel h-7 w-16" aria-hidden="true" />
      ) : (
        // Fades in when the skeleton gives way, instead of popping in.
        <span className="num in">{value ?? "–"}</span>
      )}
      {hint && <span className="xs t3">{hint}</span>}
    </div>
  );
}

export { StatCard, type StatCardProps, type StatTone };
