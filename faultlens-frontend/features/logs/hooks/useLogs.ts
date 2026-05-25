"use client";

import { useState, useCallback } from "react";
import type { LogFilters } from "@/features/logs/types/log.types";

/**
 * Log filter state management hook.
 * URL search params senkronizasyonu için genişletilebilir.
 */
export function useLogs() {
  const [filters, setFilters] = useState<LogFilters>({
    page: 0,
    size: 20,
  });

  const [liveMode, setLiveMode] = useState(false);

  const updateFilter = useCallback(
    <K extends keyof LogFilters>(key: K, value: LogFilters[K]) => {
      setFilters((prev) => ({
        ...prev,
        [key]: value,
        // Reset page when filter changes
        ...(key !== "page" ? { page: 0 } : {}),
      }));
    },
    []
  );

  const resetFilters = useCallback(() => {
    setFilters({ page: 0, size: 20 });
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
