import { useQuery } from "@tanstack/react-query";
import { LogApi } from "./logApi";
import type { LogFilters } from "@/features/logs/types/log.types";

export const logKeys = {
  all: ["logs"] as const,
  lists: () => [...logKeys.all, "list"] as const,
  list: (filters: LogFilters) => [...logKeys.lists(), filters] as const,
  detail: (id: number) => [...logKeys.all, "detail", id] as const,
  groups: () => [...logKeys.all, "groups"] as const,
  stats: () => [...logKeys.all, "stats"] as const,
};

export const useLogsQuery = (filters: LogFilters) =>
  useQuery({
    queryKey: logKeys.list(filters),
    queryFn: () => LogApi.getLogs(filters),
  });

export const useLogDetailQuery = (id: number) =>
  useQuery({
    queryKey: logKeys.detail(id),
    queryFn: () => LogApi.getLogDetail(id),
    enabled: id > 0,
  });

export const useLogGroupsQuery = () =>
  useQuery({
    queryKey: logKeys.groups(),
    queryFn: () => LogApi.getLogGroups(),
  });
