"use client";

import { useTranslations } from "next-intl";
import { BellRing, ShieldAlert, CheckCircle2, UserCheck } from "lucide-react";
import { StatCard } from "@/shared/components/ui/StatCard";
import { useAlarmStatsQuery } from "../api/useAlarmsQuery";

export function AlarmStats() {
  const t = useTranslations("alarms.stats");
  // Server-side counters: correct regardless of which list page is shown.
  const { data, isLoading } = useAlarmStatsQuery();

  return (
    <div className="grid grid-cols-2 xl:grid-cols-4 gap-4 mb-5">
      <StatCard
        label={t("active")}
        value={data?.active}
        icon={<BellRing size={16} />}
        tone="signal"
        loading={isLoading}
      />
      <StatCard
        label={t("openCritical")}
        value={data?.openCritical}
        icon={<ShieldAlert size={16} />}
        tone="crit"
        loading={isLoading}
      />
      <StatCard
        label={t("acknowledged")}
        value={data?.acknowledged}
        icon={<UserCheck size={16} />}
        tone="warn"
        loading={isLoading}
      />
      <StatCard
        label={t("resolved")}
        value={data?.resolved}
        icon={<CheckCircle2 size={16} />}
        tone="ok"
        loading={isLoading}
      />
    </div>
  );
}
