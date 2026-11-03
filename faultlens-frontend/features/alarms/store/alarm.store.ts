import { create } from "zustand";
import { AlarmRule } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";

/**
 * Alarm RULES only — still local mock data until the backend rule engine exists
 * (plan P07). Alarms themselves come from the API (see ../api).
 */
interface AlarmState {
  rules: AlarmRule[];
  addRule: (rule: Omit<AlarmRule, "id" | "createdAt">) => void;
  toggleRule: (id: string) => void;
  deleteRule: (id: string) => void;
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

export const useAlarmStore = create<AlarmState>((set) => ({
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
}));
