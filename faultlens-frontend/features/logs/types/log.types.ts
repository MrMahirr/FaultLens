import { Severity, LogSourceType } from "@/shared/types/common.types";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";

/* ── Log Entry DTO ─────────────────────────────────────────── */

export interface LogEntryDto {
  id: number;
  sourceId: number;
  groupId?: number;
  severity: Severity;
  message: string;
  parsedMessage?: string;
  stackTrace?: string;
  namespace?: string;
  podName?: string;
  containerName?: string;
  serviceName?: string;
  cluster?: string;
  timestamp: string;
}

/* ── Log Filters ───────────────────────────────────────────── */

/**
 * Time window of the log list (P08). Searches are bounded by default so they stay fast as the
 * table grows; "all" is an explicit choice (the backend also defaults to the last 24 hours).
 */
export type LogTimeRange = "1h" | "24h" | "7d" | "30d" | "all";

export const DEFAULT_LOG_TIME_RANGE: LogTimeRange = "24h";

export const LOG_TIME_RANGE_LABELS: Record<LogTimeRange, string> = {
  "1h": "Son 1 saat",
  "24h": "Son 24 saat",
  "7d": "Son 7 gün",
  "30d": "Son 30 gün",
  all: "Tümü",
};

const RANGE_MS: Record<Exclude<LogTimeRange, "all">, number> = {
  "1h": 3_600_000,
  "24h": 86_400_000,
  "7d": 7 * 86_400_000,
  "30d": 30 * 86_400_000,
};

/** ISO start of the window ending now; "all" starts at the epoch (an explicit unbounded query). */
export function rangeStart(range: LogTimeRange, now = Date.now()): string {
  return range === "all"
    ? new Date(0).toISOString()
    : new Date(now - RANGE_MS[range]).toISOString();
}

export interface LogFilters {
  severity?: Severity[];
  source?: string;
  namespace?: string;
  search?: string;
  /** Window ending now; ignored when startDate is set. */
  range?: LogTimeRange;
  startDate?: string;
  endDate?: string;
  groupId?: number;
  page?: number;
  size?: number;
}

/* ── Log Group ─────────────────────────────────────────────── */

export interface LogGroupDto {
  id: number;
  fingerprint: string;
  firstMessage: string;
  count: number;
  severity: Severity;
  firstSeenAt: string;
  lastSeenAt: string;
}

/* ── Log Stats ─────────────────────────────────────────────── */

export interface LogStatsDto {
  totalLogs: number;
  errorCount: number;
  warnCount: number;
  criticalCount: number;
}

/* ── Log Detail DTO ────────────────────────────────────────── */

export interface LogDetailDto {
  log: LogEntryDto;
  analyses: AnalysisResultDto[];
}
