import type { Severity } from "@/shared/types/common.types";

export interface DashboardStats {
  /** WARN + ERROR + CRITICAL in the last hour. */
  totalErrors: number;
  activeSources: number;
  totalSources: number;
  openGroups: number;
  analysesTotal: number;
}

export interface SeverityCount {
  severity: Severity;
  count: number;
}
