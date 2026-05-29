import { Severity, LogSourceType } from "@/shared/types/common.types";

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

export interface LogFilters {
  severity?: Severity[];
  source?: string;
  namespace?: string;
  search?: string;
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
  analyses: any[];
}
