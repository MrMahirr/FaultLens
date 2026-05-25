import { Severity, LogSourceType } from "@/shared/types/common.types";

/* ── Log Entry DTO ─────────────────────────────────────────── */

export interface LogEntryDto {
  id: number;
  severity: Severity;
  message: string;
  source: string;
  sourceType: LogSourceType;
  namespace: string;
  timestamp: string;
  stackTrace?: string;
  groupId?: number;
  podName?: string;
  containerName?: string;
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
  firstSeen: string;
  lastSeen: string;
  source: string;
  hasAnalysis: boolean;
}

/* ── Log Stats ─────────────────────────────────────────────── */

export interface LogStatsDto {
  totalLogs: number;
  errorCount: number;
  warnCount: number;
  criticalCount: number;
}
