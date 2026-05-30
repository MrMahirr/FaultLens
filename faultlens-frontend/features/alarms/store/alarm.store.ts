import { create } from "zustand";
import { Alarm, AlarmRule, AlarmStatus } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";

interface AlarmState {
  alarms: Alarm[];
  rules: AlarmRule[];
  addRule: (rule: Omit<AlarmRule, "id" | "createdAt">) => void;
  toggleRule: (id: string) => void;
  deleteRule: (id: string) => void;
  resolveAlarm: (id: string) => void;
  deleteAlarm: (id: string) => void;
}

// Initial mock data
const initialRules: AlarmRule[] = [
  {
    id: "r1",
    name: "High Error Rate",
    condition: "ERROR > 50 in 5m",
    severity: Severity.CRITICAL,
    isActive: true,
    createdAt: new Date().toISOString(),
  },
  {
    id: "r2",
    name: "Database Connection Lost",
    condition: "Log contains 'Connection refused'",
    severity: Severity.CRITICAL,
    isActive: true,
    createdAt: new Date().toISOString(),
  },
  {
    id: "r3",
    name: "High CPU Warning",
    condition: "WARN contains 'CPU > 80%'",
    severity: Severity.WARN,
    isActive: false,
    createdAt: new Date().toISOString(),
  },
];

const initialAlarms: Alarm[] = [
  {
    id: "a1",
    ruleId: "r1",
    ruleName: "High Error Rate",
    severity: Severity.CRITICAL,
    status: AlarmStatus.ACTIVE,
    message: "124 ERROR logs detected in the last 5 minutes.",
    triggeredAt: new Date(Date.now() - 1000 * 60 * 15).toISOString(), // 15 mins ago
  },
  {
    id: "a2",
    ruleId: "r2",
    ruleName: "Database Connection Lost",
    severity: Severity.CRITICAL,
    status: AlarmStatus.ACTIVE,
    message: "Connection refused to jdbc:postgresql://localhost:5432",
    triggeredAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), // 2 hours ago
  },
  {
    id: "a3",
    ruleId: "r1",
    ruleName: "High Error Rate",
    severity: Severity.CRITICAL,
    status: AlarmStatus.RESOLVED,
    message: "55 ERROR logs detected.",
    triggeredAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(), // 1 day ago
    resolvedAt: new Date(Date.now() - 1000 * 60 * 60 * 23).toISOString(),
  },
];

export const useAlarmStore = create<AlarmState>((set) => ({
  alarms: initialAlarms,
  rules: initialRules,
  addRule: (ruleData) =>
    set((state) => ({
      rules: [
        ...state.rules,
        {
          ...ruleData,
          id: Math.random().toString(36).substring(2, 9),
          createdAt: new Date().toISOString(),
        },
      ],
    })),
  toggleRule: (id) =>
    set((state) => ({
      rules: state.rules.map((r) =>
        r.id === id ? { ...r, isActive: !r.isActive } : r
      ),
    })),
  deleteRule: (id) =>
    set((state) => ({
      rules: state.rules.filter((r) => r.id !== id),
    })),
  resolveAlarm: (id) =>
    set((state) => ({
      alarms: state.alarms.map((a) =>
        a.id === id
          ? { ...a, status: AlarmStatus.RESOLVED, resolvedAt: new Date().toISOString() }
          : a
      ),
    })),
  deleteAlarm: (id) =>
    set((state) => ({
      alarms: state.alarms.filter((a) => a.id !== id),
    })),
}));
