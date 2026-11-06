import { Severity } from "@/shared/types/common.types";

export enum AlarmStatus {
  ACTIVE = "ACTIVE",
  ACKNOWLEDGED = "ACKNOWLEDGED",
  RESOLVED = "RESOLVED",
}

/** Backend AlarmRuleType (ADR 0001). */
export enum AlarmRuleType {
  THRESHOLD = "THRESHOLD",
  PATTERN = "PATTERN",
  NEW_GROUP = "NEW_GROUP",
}

export enum AlarmChannel {
  IN_APP = "IN_APP",
  SLACK = "SLACK",
  EMAIL = "EMAIL",
  WEBHOOK = "WEBHOOK",
}

/** Backend AlarmRuleResponse (camelCase via the axios interceptor). */
export interface AlarmRule {
  id: number;
  name: string;
  type: AlarmRuleType;
  /** Minimum severity an entry (or new group) needs to count. */
  severity: Severity;
  windowSec: number | null;
  threshold: number | null;
  pattern: string | null;
  /** Empty = all sources. */
  sourceIds: number[];
  channels: AlarmChannel[];
  cooldownSec: number;
  enabled: boolean;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Backend AlarmRuleRequest. */
export interface AlarmRuleInput {
  name: string;
  type: AlarmRuleType;
  severity: Severity;
  windowSec?: number;
  threshold?: number;
  pattern?: string;
  sourceIds: number[];
  channels: AlarmChannel[];
  cooldownSec: number;
  enabled?: boolean;
}

export interface Alarm {
  id: number;
  ruleId?: string;
  ruleName: string;
  sourceId?: number;
  severity: Severity;
  status: AlarmStatus;
  message: string;
  triggeredAt: string;
  resolvedAt?: string;
  /** Rule that raised the alarm; null for built-in alarms (e.g. AI analysis). */
  alarmRuleId?: number | null;
}

/** Backend AlarmStatsDto: counters independent of list pagination. */
export interface AlarmStats {
  active: number;
  acknowledged: number;
  resolved: number;
  /** CRITICAL alarms that are ACTIVE or ACKNOWLEDGED */
  openCritical: number;
}

export interface AlarmFilters {
  status?: AlarmStatus;
  severity?: Severity;
  page?: number;
  size?: number;
}
