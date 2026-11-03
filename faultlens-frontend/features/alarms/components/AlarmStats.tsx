"use client";

import { BellRing, ShieldAlert, CheckCircle2, UserCheck } from "lucide-react";
import { useAlarmStatsQuery } from "../api/useAlarmsQuery";

export function AlarmStats() {
  // Server-side counters: correct regardless of which list page is shown.
  const { data } = useAlarmStatsQuery();

  const stats = [
    {
      title: "Aktif Alarmlar",
      value: data?.active,
      icon: <BellRing size={24} className="text-accent" />,
      bg: "bg-accent/10",
      border: "border-accent/20",
    },
    {
      title: "Açık Kritik",
      value: data?.openCritical,
      icon: <ShieldAlert size={24} className="text-error" />,
      bg: "bg-error/10",
      border: "border-error/20",
    },
    {
      title: "Üstlenilenler",
      value: data?.acknowledged,
      icon: <UserCheck size={24} className="text-warning" />,
      bg: "bg-warning/10",
      border: "border-warning/20",
    },
    {
      title: "Çözülenler",
      value: data?.resolved,
      icon: <CheckCircle2 size={24} className="text-success" />,
      bg: "bg-success/10",
      border: "border-success/20",
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
      {stats.map((stat) => (
        <div
          key={stat.title}
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
              {stat.value ?? "–"}
            </h3>
          </div>
        </div>
      ))}
    </div>
  );
}
