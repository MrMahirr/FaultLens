import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import type { DashboardStats, ChartPoint } from "../types/dashboard.types";
import { HttpMethod } from "@/shared/api/methods";

export const DashboardApi = {
  getStats: async (): Promise<DashboardStats> => {
    const [statsRes, sourcesRes, groupsRes, analysesRes] = await Promise.all([
      apiClient({ method: HttpMethod.GET, url: Endpoints.LOGS.STATS }),
      apiClient({ method: HttpMethod.GET, url: Endpoints.SOURCES.LIST }),
      apiClient({ method: HttpMethod.GET, url: Endpoints.LOGS.GROUPS }),
      apiClient({ method: HttpMethod.GET, url: Endpoints.ANALYSES.LIST }),
    ]);

    const statsData = statsRes.data?.data || {};
    const sourcesList = sourcesRes.data?.data || [];
    const groupsPage = groupsRes.data?.data || {};
    const analysesList = analysesRes.data?.data?.content || analysesRes.data?.data || [];

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
  },

  getSeverityChart: async (): Promise<ChartPoint[]> => {
    const statsRes = await apiClient({
      method: HttpMethod.GET,
      url: `${Endpoints.LOGS.STATS}?windowMinutes=60`,
    });
    const statsData = statsRes.data?.data || {};

    const critical = Number(statsData["CRITICAL"] || 0);
    const error = Number(statsData["ERROR"] || 0);
    const warn = Number(statsData["WARN"] || 0);
    const info = Number(statsData["INFO"] || 0);

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
  },
};
