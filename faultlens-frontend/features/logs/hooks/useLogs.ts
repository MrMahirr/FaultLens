"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useUrlState } from "@/shared/hooks/useUrlState";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { Severity } from "@/shared/types/common.types";
import {
  DEFAULT_LOG_TIME_RANGE,
  LOG_TIME_RANGES,
  type LogFilters,
  type LogTimeRange,
} from "@/features/logs/types/log.types";

/** Rows per page; the table is virtualised, so a page can be long. */
export const LOG_PAGE_SIZE = 100;
const SEARCH_DEBOUNCE_MS = 300;

/** Query-string keys: /logs?q=timeout&sev=ERROR&src=3&range=7d&page=2 */
const KEYS = { search: "q", severity: "sev", source: "src", range: "range", page: "page" } as const;

function parseSeverity(value: string | null): Severity | undefined {
  return value && (Object.values(Severity) as string[]).includes(value)
    ? (value as Severity)
    : undefined;
}

function parseRange(value: string | null): LogTimeRange {
  return value && (LOG_TIME_RANGES as string[]).includes(value)
    ? (value as LogTimeRange)
    : DEFAULT_LOG_TIME_RANGE;
}

/**
 * Log filters kept in the URL, so a filtered view is a shareable link. The search box updates
 * at once; the URL (and so the API query) follows when typing pauses.
 */
export function useLogs() {
  const { params, update, clear } = useUrlState();

  const filters = useMemo<LogFilters>(() => {
    const page = Number(params.get(KEYS.page));
    return {
      search: params.get(KEYS.search) ?? undefined,
      severity: parseSeverity(params.get(KEYS.severity)),
      source: params.get(KEYS.source) ?? undefined,
      range: parseRange(params.get(KEYS.range)),
      page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
      size: LOG_PAGE_SIZE,
    };
  }, [params]);

  const [searchInput, setSearchInput] = useState(filters.search ?? "");
  const debouncedSearch = useDebounce(searchInput, SEARCH_DEBOUNCE_MS);

  useEffect(() => {
    if ((debouncedSearch || undefined) !== filters.search) {
      update({ [KEYS.search]: debouncedSearch, [KEYS.page]: undefined });
    }
  }, [debouncedSearch, filters.search, update]);

  const [liveMode, setLiveMode] = useState(false);

  const updateFilter = useCallback(
    <K extends keyof LogFilters>(key: K, value: LogFilters[K]) => {
      if (key === "search") {
        setSearchInput((value as string | undefined) ?? "");
        return;
      }
      const urlKey = KEYS[key as keyof typeof KEYS];
      if (!urlKey) return;
      let urlValue: string | number | undefined =
        value === undefined ? undefined : (value as string | number);
      if (key === "page") urlValue = (value as number) > 0 ? (value as number) + 1 : undefined;
      if (key === "range" && value === DEFAULT_LOG_TIME_RANGE) urlValue = undefined;
      // Any filter other than the page sends the user back to the first page.
      update(
        key === "page" ? { [urlKey]: urlValue } : { [urlKey]: urlValue, [KEYS.page]: undefined },
      );
    },
    [update],
  );

  const resetFilters = useCallback(() => {
    setSearchInput("");
    clear(Object.values(KEYS));
  }, [clear]);

  const toggleLiveMode = useCallback(() => setLiveMode((prev) => !prev), []);

  return {
    /** Filters as applied to the API (search is the committed, debounced value). */
    filters,
    searchInput,
    updateFilter,
    resetFilters,
    liveMode,
    toggleLiveMode,
  };
}
