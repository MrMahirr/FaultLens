import { Severity } from "@/shared/types/common.types";

export enum AlarmStatus {
  ACTIVE = "ACTIVE",
  ACKNOWLEDGED = "ACKNOWLEDGED",
  RESOLVED = "RESOLVED",
}

export const ALARM_STATUS_LABELS: Record<AlarmStatus, string> = {
  [AlarmStatus.ACTIVE]: "Aktif",
  [AlarmStatus.ACKNOWLEDGED]: "Üstlenildi",
  [AlarmStatus.RESOLVED]: "Çözüldü",
};

export interface AlarmRule {
  id: string;
  name: string;
  sourceId?: number; // Optional, if empty it applies to all sources
  condition: string; // e.g., "count(Severity.ERROR) > 50 in 5m"
  severity: Severity;
  isActive: boolean;
  createdAt: string;
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
