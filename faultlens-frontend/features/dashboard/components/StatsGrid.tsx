"use client";

import { useTranslations } from "next-intl";
import { AlertTriangle, Server, Layers, Brain } from "lucide-react";
import { StatCard } from "@/shared/components/ui/StatCard";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useDashboardStatsQuery } from "@/features/dashboard/api/useDashboardQuery";

function StatsGrid() {
  const t = useTranslations("dashboard.stats");
  const { number } = useDateFormat();
  const { data: stats, isLoading } = useDashboardStatsQuery();

  return (
    <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
      <StatCard
        label={t("errors")}
        value={stats ? number(stats.totalErrors) : undefined}
        hint={t("errorsHint")}
        icon={<AlertTriangle size={16} />}
        tone="crit"
        loading={isLoading}
      />
      <StatCard
        label={t("sources")}
        value={stats ? `${stats.activeSources}/${stats.totalSources}` : undefined}
        hint={t("sourcesHint")}
        icon={<Server size={16} />}
        tone="ok"
        loading={isLoading}
      />
      <StatCard
        label={t("groups")}
        value={stats ? number(stats.openGroups) : undefined}
        hint={t("groupsHint")}
        icon={<Layers size={16} />}
        tone="warn"
        loading={isLoading}
      />
      <StatCard
        label={t("analyses")}
        value={stats ? number(stats.analysesTotal) : undefined}
        hint={t("analysesHint")}
        icon={<Brain size={16} />}
        tone="ai"
        loading={isLoading}
      />
    </div>
  );
}

export { StatsGrid };
