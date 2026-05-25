import { useQuery } from "@tanstack/react-query";
import { mockLogEntries, mockLogGroups, mockAnalyses } from "@/shared/mocks/data";
import type { LogEntryDto, LogFilters, LogGroupDto, LogDetailDto } from "@/features/logs/types/log.types";
import type { PagedResponse } from "@/shared/types/api.types";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";

/* ── Query Keys ────────────────────────────────────────────── */

export const logKeys = {
  all: ["logs"] as const,
  lists: () => [...logKeys.all, "list"] as const,
  list: (filters: LogFilters) => [...logKeys.lists(), filters] as const,
  detail: (id: number) => [...logKeys.all, "detail", id] as const,
  groups: () => [...logKeys.all, "groups"] as const,
  stats: () => [...logKeys.all, "stats"] as const,
};

/* ── Mock Fallback Fetchers ────────────────────────────────── */

const mockFetchLogs = async (
  filters: LogFilters
): Promise<PagedResponse<LogEntryDto>> => {
  await new Promise((resolve) => setTimeout(resolve, 400));

  let filtered = [...mockLogEntries] as LogEntryDto[];

  if (filters.severity && filters.severity.length > 0) {
    filtered = filtered.filter((log) =>
      filters.severity!.includes(log.severity)
    );
  }

  if (filters.source) {
    filtered = filtered.filter((log) => log.source === filters.source);
  }

  if (filters.search) {
    const searchLower = filters.search.toLowerCase();
    filtered = filtered.filter(
      (log) =>
        log.message.toLowerCase().includes(searchLower) ||
        log.source.toLowerCase().includes(searchLower)
    );
  }

  if (filters.groupId) {
    filtered = filtered.filter((log) => log.groupId === filters.groupId);
  }

  filtered.sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
  );

  const page = filters.page ?? 0;
  const size = filters.size ?? 20;
  const start = page * size;
  const content = filtered.slice(start, start + size);

  return {
    content,
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size),
    page,
    size,
    hasNext: start + size < filtered.length,
    hasPrevious: page > 0,
  };
};

/* ── Active API Fetchers with Fallbacks ──────────────────────── */

const fetchLogs = async (
  filters: LogFilters
): Promise<PagedResponse<LogEntryDto>> => {
  try {
    const params: Record<string, any> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };

    if (filters.severity && filters.severity.length > 0) {
      params.severity = filters.severity[0];
    }
    if (filters.search) {
      params.search = filters.search;
    }
    if (filters.source) {
      const sourceIdNum = Number(filters.source);
      if (!isNaN(sourceIdNum)) {
        params.sourceId = sourceIdNum;
      }
    }
    if (filters.startDate) {
      params.from = filters.startDate;
    }
    if (filters.endDate) {
      params.to = filters.endDate;
    }

    const url = filters.groupId
      ? Endpoints.LOGS.GROUP_ENTRIES(filters.groupId)
      : Endpoints.LOGS.LIST;

    const response = await apiClient({
      method: HttpMethod.GET,
      url,
      params,
    });

    return response.data.data;
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      console.warn("Backend connection failed, falling back to mock logs.");
      return mockFetchLogs(filters);
    }
    throw error;
  }
};

const fetchLogDetail = async (id: number): Promise<LogDetailDto> => {
  try {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.DETAIL(id),
    });
    return response.data.data;
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      const log = mockLogEntries.find((l) => l.id === id) as LogEntryDto | undefined;
      if (!log) throw new Error("Log bulunamadı");
      const analyses = mockAnalyses.filter((a) => a.groupId === log.groupId);
      return { log, analyses };
    }
    throw error;
  }
};

const fetchLogGroups = async (): Promise<LogGroupDto[]> => {
  try {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.GROUPS,
    });
    return response.data.data;
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      return mockLogGroups as LogGroupDto[];
    }
    throw error;
  }
};

/* ── Hooks ─────────────────────────────────────────────────── */

export const useLogs = (filters: LogFilters) =>
  useQuery({
    queryKey: logKeys.list(filters),
    queryFn: () => fetchLogs(filters),
  });

export const useLogDetail = (id: number) =>
  useQuery({
    queryKey: logKeys.detail(id),
    queryFn: () => fetchLogDetail(id),
    enabled: id > 0,
  });

export const useLogGroups = () =>
  useQuery({
    queryKey: logKeys.groups(),
    queryFn: fetchLogGroups,
  });
