"use client";

import { useState, useCallback } from "react";
import { DEFAULT_LOG_TIME_RANGE, type LogFilters } from "@/features/logs/types/log.types";

/**
 * Log filter state management hook.
 * URL search params senkronizasyonu için genişletilebilir.
 */
export function useLogs() {
  const [filters, setFilters] = useState<LogFilters>({
    page: 0,
    size: 20,
    range: DEFAULT_LOG_TIME_RANGE,
  });

  const [liveMode, setLiveMode] = useState(false);

  const updateFilter = useCallback(<K extends keyof LogFilters>(key: K, value: LogFilters[K]) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value,
      // Reset page when filter changes
      ...(key !== "page" ? { page: 0 } : {}),
    }));
  }, []);

  const resetFilters = useCallback(() => {
    setFilters({ page: 0, size: 20, range: DEFAULT_LOG_TIME_RANGE });
  }, []);

  const toggleLiveMode = useCallback(() => {
    setLiveMode((prev) => !prev);
  }, []);

  return {
    filters,
    setFilters,
    updateFilter,
    resetFilters,
    liveMode,
    toggleLiveMode,
  };
}
