import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import type { DashboardStats, SeverityCount } from "../types/dashboard.types";
import { HttpMethod } from "@/shared/api/methods";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import { ConnectionStatus, Severity } from "@/shared/types/common.types";

const SEVERITY_ORDER = [
  Severity.CRITICAL,
  Severity.ERROR,
  Severity.WARN,
  Severity.INFO,
  Severity.DEBUG,
  Severity.TRACE,
];

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

    // "Bağlı" means the collector is streaming it right now, not merely enabled.
    const activeSources = sourcesList.filter(
      (s: LogSourceDto) => s.status === ConnectionStatus.CONNECTED,
    ).length;
    const totalSources = sourcesList.length;

    const criticalCount = Number(statsData["CRITICAL"] || 0);
    const errorCount = Number(statsData["ERROR"] || 0);
    const warnCount = Number(statsData["WARN"] || 0);
    const analysesPage = analysesRes.data?.data;

    return {
      totalErrors: criticalCount + errorCount + warnCount,
      activeSources,
      totalSources,
      openGroups: groupsPage.totalElements || groupsPage.content?.length || 0,
      // The list is paged: totalElements is the real count (not the length of page 1).
      analysesTotal:
        typeof analysesPage?.totalElements === "number"
          ? analysesPage.totalElements
          : analysesList.length,
    };
  },

  /** Per-severity log counts of the last hour (real counts from /logs/stats). */
  getSeverityCounts: async (): Promise<SeverityCount[]> => {
    const statsRes = await apiClient({
      method: HttpMethod.GET,
      url: `${Endpoints.LOGS.STATS}?windowMinutes=60`,
    });
    const statsData = statsRes.data?.data || {};
    return SEVERITY_ORDER.map((severity) => ({
      severity,
      count: Number(statsData[severity] || 0),
    }));
  },
};
