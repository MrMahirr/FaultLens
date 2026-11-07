import { useQuery } from "@tanstack/react-query";
import { DashboardApi } from "./dashboardApi";

export const dashboardKeys = {
  all: ["dashboard"] as const,
  stats: () => [...dashboardKeys.all, "stats"] as const,
  severity: () => [...dashboardKeys.all, "severity"] as const,
};

export const useDashboardStatsQuery = () =>
  useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: () => DashboardApi.getStats(),
  });

export const useSeverityCountsQuery = () =>
  useQuery({
    queryKey: dashboardKeys.severity(),
    queryFn: () => DashboardApi.getSeverityCounts(),
    refetchInterval: 30_000,
  });
