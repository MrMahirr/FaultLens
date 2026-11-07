"use client";

import { useTranslations } from "next-intl";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { AiSummaryWidget } from "@/features/dashboard/components/AiSummaryWidget";
import { StatsGrid } from "@/features/dashboard/components/StatsGrid";
import { SeverityChart } from "@/features/dashboard/components/SeverityChart";
import { RealtimeFeed } from "@/features/dashboard/components/RealtimeFeed";
import { TopErrorsWidget } from "@/features/dashboard/components/TopErrorsWidget";
import { SystemHealthMap } from "@/features/dashboard/components/SystemHealthMap";

export default function DashboardPage() {
  const t = useTranslations("dashboard");

  return (
    <>
      <PageHeader title={t("title")} description={t("subtitle")} />

      <StatsGrid />

      <AiSummaryWidget />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        <div className="lg:col-span-7">
          <SeverityChart />
        </div>
        <div className="lg:col-span-5">
          <RealtimeFeed />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <TopErrorsWidget />
        <SystemHealthMap />
      </div>
    </>
  );
}
