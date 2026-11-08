import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { LogApi } from "./logApi";
import type { LogFilters, LogGroupListParams } from "@/features/logs/types/log.types";

export const logKeys = {
  all: ["logs"] as const,
  lists: () => [...logKeys.all, "list"] as const,
  list: (filters: LogFilters) => [...logKeys.lists(), filters] as const,
  detail: (id: number) => [...logKeys.all, "detail", id] as const,
  groups: () => [...logKeys.all, "groups"] as const,
  groupList: (params: LogGroupListParams) => [...logKeys.groups(), "list", params] as const,
  group: (id: number) => [...logKeys.groups(), "detail", id] as const,
  groupEvents: (id: number) => [...logKeys.groups(), "events", id] as const,
  groupComments: (id: number) => [...logKeys.groups(), "comments", id] as const,
  assignees: () => [...logKeys.groups(), "assignees"] as const,
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

/**
 * Polls so a regression (a resolved group reopened by new logs) shows up without a reload; the
 * previous page stays on screen while the next one loads.
 */
export const useLogGroupsQuery = (params: LogGroupListParams = {}) =>
  useQuery({
    queryKey: logKeys.groupList(params),
    queryFn: () => LogApi.getLogGroups(params),
    placeholderData: keepPreviousData,
    refetchInterval: 15_000,
  });

export const useLogGroupQuery = (id: number) =>
  useQuery({
    queryKey: logKeys.group(id),
    queryFn: () => LogApi.getLogGroup(id),
    enabled: id > 0,
    refetchInterval: 15_000,
  });

export const useLogGroupEventsQuery = (id: number) =>
  useQuery({
    queryKey: logKeys.groupEvents(id),
    queryFn: () => LogApi.getLogGroupEvents(id),
    enabled: id > 0,
  });

export const useLogGroupCommentsQuery = (id: number) =>
  useQuery({
    queryKey: logKeys.groupComments(id),
    queryFn: () => LogApi.getLogGroupComments(id),
    enabled: id > 0,
  });

/** Only for editors (the API refuses viewers). */
export const useAssignableUsersQuery = (enabled: boolean) =>
  useQuery({
    queryKey: logKeys.assignees(),
    queryFn: () => LogApi.getAssignableUsers(),
    enabled,
    staleTime: 5 * 60_000,
  });
