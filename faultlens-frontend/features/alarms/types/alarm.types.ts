import { Severity } from "@/shared/types/common.types";

export enum AlarmStatus {
  ACTIVE = "ACTIVE",
  ACKNOWLEDGED = "ACKNOWLEDGED",
  RESOLVED = "RESOLVED",
}

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
  id: string | number;
  ruleId: string;
  ruleName: string;
  sourceId?: number;
  severity: Severity;
  status: AlarmStatus;
  message: string;
  triggeredAt: string;
  resolvedAt?: string;
}

export interface AlarmStats {
  totalActive: number;
  criticalCount: number;
  resolvedCount: number;
}
