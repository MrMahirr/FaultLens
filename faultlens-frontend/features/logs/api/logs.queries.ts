import { useQuery } from "@tanstack/react-query";
import { mockLogEntries, mockLogGroups } from "@/shared/mocks/data";
import type { LogEntryDto, LogFilters, LogGroupDto } from "@/features/logs/types/log.types";
import type { PagedResponse } from "@/shared/types/api.types";

/* ── Query Keys ────────────────────────────────────────────── */

export const logKeys = {
  all: ["logs"] as const,
  lists: () => [...logKeys.all, "list"] as const,
  list: (filters: LogFilters) => [...logKeys.lists(), filters] as const,
  detail: (id: number) => [...logKeys.all, "detail", id] as const,
  groups: () => [...logKeys.all, "groups"] as const,
  stats: () => [...logKeys.all, "stats"] as const,
};

/* ── Mock Fetchers ─────────────────────────────────────────── */

const fetchLogs = async (
  filters: LogFilters
): Promise<PagedResponse<LogEntryDto>> => {
  await new Promise((resolve) => setTimeout(resolve, 400));

  let filtered = [...mockLogEntries] as LogEntryDto[];

  // Apply severity filter
  if (filters.severity && filters.severity.length > 0) {
    filtered = filtered.filter((log) =>
      filters.severity!.includes(log.severity)
    );
  }

  // Apply source filter
  if (filters.source) {
    filtered = filtered.filter((log) => log.source === filters.source);
  }

  // Apply search filter
  if (filters.search) {
    const searchLower = filters.search.toLowerCase();
    filtered = filtered.filter(
      (log) =>
        log.message.toLowerCase().includes(searchLower) ||
        log.source.toLowerCase().includes(searchLower)
    );
  }

  // Apply group filter
  if (filters.groupId) {
    filtered = filtered.filter((log) => log.groupId === filters.groupId);
  }

  // Sort by timestamp descending
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

const fetchLogDetail = async (id: number): Promise<LogEntryDto> => {
  await new Promise((resolve) => setTimeout(resolve, 300));
  const log = mockLogEntries.find((l) => l.id === id) as LogEntryDto | undefined;
  if (!log) throw new Error("Log bulunamadı");
  return log;
};

const fetchLogGroups = async (): Promise<LogGroupDto[]> => {
  await new Promise((resolve) => setTimeout(resolve, 400));
  return mockLogGroups as LogGroupDto[];
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
