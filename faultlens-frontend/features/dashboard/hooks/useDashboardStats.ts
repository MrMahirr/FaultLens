import { useQuery } from "@tanstack/react-query";
import {
  mockDashboardStats,
  type MockDashboardStats,
  mockSeverityChartData,
  type MockChartPoint,
} from "@/shared/mocks/data";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";

/* ── Query Keys ────────────────────────────────────────────── */

export const dashboardKeys = {
  all: ["dashboard"] as const,
  stats: () => [...dashboardKeys.all, "stats"] as const,
  chart: () => [...dashboardKeys.all, "chart"] as const,
};

/* ── Active API Fetchers with Fallbacks ──────────────────────── */

const fetchDashboardStats = async (): Promise<MockDashboardStats> => {
  try {
    const [statsRes, sourcesRes, groupsRes, analysesRes] = await Promise.all([
      apiClient.get(Endpoints.LOGS.STATS),
      apiClient.get(Endpoints.SOURCES.LIST),
      apiClient.get(Endpoints.LOGS.GROUPS),
      apiClient.get(Endpoints.ANALYSES.LIST),
    ]);

    const statsData = statsRes.data?.data || {};
    const sourcesList = sourcesRes.data?.data || [];
    const groupsPage = groupsRes.data?.data || {};
    const analysesList = analysesRes.data?.data || [];

    const activeSources = sourcesList.filter((s: any) => s.status === "CONNECTED" || s.enabled).length;
    const totalSources = sourcesList.length;

    const criticalCount = Number(statsData["CRITICAL"] || 0);
    const errorCount = Number(statsData["ERROR"] || 0);
    const warnCount = Number(statsData["WARN"] || 0);

    return {
      totalErrors: criticalCount + errorCount + warnCount,
      totalErrorsChange: 0,
      activeSources,
      totalSources,
      openGroups: groupsPage.totalElements || groupsPage.content?.length || 0,
      analysesToday: analysesList.length,
    };
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      console.warn("Backend connection failed, falling back to mock stats.");
      return mockDashboardStats;
    }
    throw error;
  }
};

const fetchSeverityChart = async (): Promise<MockChartPoint[]> => {
  try {
    const statsRes = await apiClient.get(`${Endpoints.LOGS.STATS}?windowMinutes=60`);
    const statsData = statsRes.data?.data || {};

    const critical = Number(statsData["CRITICAL"] || 0);
    const error = Number(statsData["ERROR"] || 0);
    const warn = Number(statsData["WARN"] || 0);
    const info = Number(statsData["INFO"] || 0);

    // Distribute total counts into a visual trend over 6 intervals
    return Array.from({ length: 6 }, (_, i) => {
      const factor = (i + 1) / 6;
      return {
        time: `${30 - i * 5}m`,
        critical: Math.max(0, Math.floor(critical * factor * 0.4)),
        error: Math.max(0, Math.floor(error * factor * 0.4)),
        warn: Math.max(0, Math.floor(warn * factor * 0.4)),
        info: Math.max(0, Math.floor(info * factor * 0.4)),
      };
    }).reverse();
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      console.warn("Backend connection failed, falling back to mock chart.");
      return mockSeverityChartData;
    }
    throw error;
  }
};

/* ── Hooks ─────────────────────────────────────────────────── */

export const useDashboardStats = () =>
  useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: fetchDashboardStats,
  });

export const useSeverityChart = () =>
  useQuery({
    queryKey: dashboardKeys.chart(),
    queryFn: fetchSeverityChart,
  });
