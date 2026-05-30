"use client";

import { BellRing, ShieldAlert, CheckCircle2 } from "lucide-react";
import { useAlarmsQuery } from "../api/useAlarmsQuery";
import { AlarmStatus } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";

export function AlarmStats() {
  const { data: alarms = [] } = useAlarmsQuery();

  const activeAlarms = alarms.filter((a) => a.status === AlarmStatus.ACTIVE);
  const criticalAlarms = activeAlarms.filter(
    (a) => a.severity === Severity.CRITICAL || a.severity === Severity.ERROR
  );
  const resolvedAlarms = alarms.filter(
    (a) => a.status === AlarmStatus.RESOLVED
  );

  const stats = [
    {
      title: "Aktif Alarmlar",
      value: activeAlarms.length,
      icon: <BellRing size={24} className="text-accent" />,
      bg: "bg-accent/10",
      border: "border-accent/20",
    },
    {
      title: "Kritik & Hata",
      value: criticalAlarms.length,
      icon: <ShieldAlert size={24} className="text-error" />,
      bg: "bg-error/10",
      border: "border-error/20",
    },
    {
      title: "Çözülenler",
      value: resolvedAlarms.length,
      icon: <CheckCircle2 size={24} className="text-success" />,
      bg: "bg-success/10",
      border: "border-success/20",
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      {stats.map((stat, i) => (
        <div
          key={i}
          className={`flex items-center p-6 rounded-2xl border ${stat.bg} ${stat.border} backdrop-blur-md transition-transform duration-300 hover:-translate-y-1 hover:shadow-lg`}
        >
          <div className="p-4 rounded-xl bg-bg-primary/50 shadow-inner mr-5">
            {stat.icon}
          </div>
          <div>
            <p className="text-sm text-text-muted font-medium mb-1">
              {stat.title}
            </p>
            <h3 className="text-3xl font-bold text-text-primary tracking-tight">
              {stat.value}
            </h3>
          </div>
        </div>
      ))}
    </div>
  );
}
