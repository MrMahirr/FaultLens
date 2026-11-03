import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { AlarmApi } from "./alarmApi";
import type { AlarmFilters } from "../types/alarm.types";

export const alarmKeys = {
  all: ["alarms"] as const,
  lists: () => [...alarmKeys.all, "list"] as const,
  list: (filters: AlarmFilters) => [...alarmKeys.lists(), filters] as const,
  stats: () => [...alarmKeys.all, "stats"] as const,
};

/** New alarms arrive from the backend pipeline, so both views poll. */
const REFETCH_INTERVAL_MS = 30_000;

export const useAlarmsQuery = (filters: AlarmFilters) =>
  useQuery({
    queryKey: alarmKeys.list(filters),
    queryFn: () => AlarmApi.list(filters),
    placeholderData: keepPreviousData,
    refetchInterval: REFETCH_INTERVAL_MS,
  });

export const useAlarmStatsQuery = () =>
  useQuery({
    queryKey: alarmKeys.stats(),
    queryFn: AlarmApi.stats,
    refetchInterval: REFETCH_INTERVAL_MS,
  });
