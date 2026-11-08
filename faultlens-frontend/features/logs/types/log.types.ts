import { Severity } from "@/shared/types/common.types";
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

export const LOG_TIME_RANGES: LogTimeRange[] = ["1h", "24h", "7d", "30d", "all"];

export const DEFAULT_LOG_TIME_RANGE: LogTimeRange = "24h";

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
  /** Exact severity (the API filters on one level). */
  severity?: Severity;
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

/** Backend GroupStatus (P10 / 2.1, ADR 0004). */
export enum GroupStatus {
  UNRESOLVED = "UNRESOLVED",
  RESOLVED = "RESOLVED",
  IGNORED = "IGNORED",
}

export interface LogGroupDto {
  id: number;
  fingerprint: string;
  firstMessage: string;
  count: number;
  severity: Severity;
  firstSeenAt: string;
  lastSeenAt: string;
  sourceId: number | null;
  status: GroupStatus;
  /** When it was resolved or ignored; null while unresolved. */
  resolvedAt: string | null;
  /** Username of whoever resolved or ignored it. */
  resolvedBy: string | null;
  /** Ignored until this time; null = until reopened. */
  ignoreUntil: string | null;
  /** Last time it was seen again after being resolved. */
  regressedAt: string | null;
  regressionCount: number;
  /** Username of the person working on it (2.2). */
  assignee: string | null;
  assignedAt: string | null;
  /** Merged into this group: its new entries go there. */
  mergedIntoId: number | null;
  mergedAt: string | null;
}

/** Backend LogGroupEventType: one line of a group's timeline. */
export enum LogGroupEventType {
  CREATED = "CREATED",
  RESOLVED = "RESOLVED",
  IGNORED = "IGNORED",
  REOPENED = "REOPENED",
  REGRESSED = "REGRESSED",
  UNIGNORED = "UNIGNORED",
  ASSIGNED = "ASSIGNED",
  UNASSIGNED = "UNASSIGNED",
  COMMENTED = "COMMENTED",
  MERGED = "MERGED",
  MERGED_FROM = "MERGED_FROM",
  UNMERGED = "UNMERGED",
}

export interface LogGroupEventDto {
  id: number;
  type: LogGroupEventType;
  /** Username; null for system transitions (regression, ignore expiry). */
  actor: string | null;
  fromStatus: GroupStatus | null;
  ignoreUntil: string | null;
  regressionCount: number | null;
  /** ASSIGNED: who it was assigned to. */
  assignee: string | null;
  /** MERGED, MERGED_FROM, UNMERGED: the other group. */
  relatedGroupId: number | null;
  createdAt: string;
}

export interface LogGroupCommentDto {
  id: number;
  groupId: number;
  author: string;
  /** Plain text, shown as text (never as HTML). */
  body: string;
  createdAt: string;
  editedAt: string | null;
}

export interface AssignableUser {
  username: string;
}

export interface GroupStatusUpdate {
  status: GroupStatus;
  /** Only with IGNORED; omitted = until reopened. */
  ignoreUntil?: string;
}

export interface LogGroupListParams {
  statuses?: GroupStatus[];
  /** Only groups assigned to this username. */
  assignee?: string;
  severity?: Severity;
  page?: number;
  size?: number;
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

/** GET /logs/{id}/context (P10 / 2.3): same source and pod, oldest first, the anchor included. */
export interface LogContextDto {
  anchorId: number;
  entries: LogEntryDto[];
  hasMoreBefore: boolean;
  hasMoreAfter: boolean;
  /** Entries are looked for within ± this many minutes of the anchor. */
  windowMinutes: number;
}
