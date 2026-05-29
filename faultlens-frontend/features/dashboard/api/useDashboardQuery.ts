import { useQuery } from "@tanstack/react-query";
import { DashboardApi } from "./dashboardApi";

export const dashboardKeys = {
  all: ["dashboard"] as const,
  stats: () => [...dashboardKeys.all, "stats"] as const,
  chart: () => [...dashboardKeys.all, "chart"] as const,
};

export const useDashboardStatsQuery = () =>
  useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: () => DashboardApi.getStats(),
  });

export const useSeverityChartQuery = () =>
  useQuery({
    queryKey: dashboardKeys.chart(),
    queryFn: () => DashboardApi.getSeverityChart(),
  });
