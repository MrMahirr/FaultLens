"use client";

import { PageHeader } from "@/shared/components/layout/PageHeader";
import { AiSummaryWidget } from "@/features/dashboard/components/AiSummaryWidget";
import { StatsGrid } from "@/features/dashboard/components/StatsGrid";
import { SeverityChart } from "@/features/dashboard/components/SeverityChart";
import { RealtimeFeed } from "@/features/dashboard/components/RealtimeFeed";
import { TopErrorsWidget } from "@/features/dashboard/components/TopErrorsWidget";
import { SystemHealthMap } from "@/features/dashboard/components/SystemHealthMap";

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Sistem durumunu ve log aktivitesini izleyin"
      />

      {/* AI Summary */}
      <AiSummaryWidget />

      {/* Stats Grid */}
      <StatsGrid />

      {/* Middle Row: Chart + Realtime Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <div className="lg:col-span-3">
          <SeverityChart />
        </div>
        <div className="lg:col-span-2">
          <RealtimeFeed />
        </div>
      </div>

      {/* Bottom Row: Top Errors + System Health */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TopErrorsWidget />
        <SystemHealthMap />
      </div>
    </div>
  );
}
