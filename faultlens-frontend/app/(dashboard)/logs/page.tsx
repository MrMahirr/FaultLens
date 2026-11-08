"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { AnimatePresence } from "framer-motion";
import { useTranslations } from "next-intl";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { LogsNav } from "@/features/logs/components/LogsNav";
import { Pagination } from "@/shared/components/ui/Pagination";
import { LogTable } from "@/features/logs/components/LogTable";
import { LogFiltersBar } from "@/features/logs/components/LogFilters";
import { LogDetailPanel } from "@/features/logs/components/LogDetailPanel";
import { useLogs } from "@/features/logs/hooks/useLogs";
import { useLogsQuery } from "@/features/logs/api/useLogQuery";
import { useLogStream } from "@/features/logs/hooks/useLogStream";
import type { LogEntryDto, LogFilters } from "@/features/logs/types/log.types";

/** Upper bound of rows kept from the live stream (virtualised, so this stays smooth). */
const LIVE_BUFFER_MAX = 5000;

/** Client-side mirror of the list query, so streamed rows respect the active filters. */
function matches(log: LogEntryDto, filters: LogFilters): boolean {
  if (filters.severity && log.severity !== filters.severity) return false;
  if (filters.source && String(log.sourceId) !== filters.source) return false;
  if (filters.search && !log.message?.toLowerCase().includes(filters.search.toLowerCase()))
    return false;
  return true;
}

function LogsView() {
  const t = useTranslations("logs");
  const { filters, searchInput, updateFilter, resetFilters, liveMode, toggleLiveMode } = useLogs();
  const { data, isLoading } = useLogsQuery(filters);
  const [selectedLog, setSelectedLog] = useState<LogEntryDto | null>(null);

  // Rows received over the WebSocket while live mode is on; merged on top of page 1.
  const [liveRows, setLiveRows] = useState<LogEntryDto[]>([]);
  const [freshIds, setFreshIds] = useState<ReadonlySet<number>>(new Set());

  const onNewLog = useCallback(
    (log: LogEntryDto) => {
      if (!matches(log, filters)) return;
      setLiveRows((prev) =>
        prev.some((l) => l.id === log.id) ? prev : [log, ...prev].slice(0, LIVE_BUFFER_MAX),
      );
      setFreshIds((prev) => new Set(prev).add(log.id));
    },
    [filters],
  );

  useLogStream({ enabled: liveMode && (filters.page ?? 0) === 0, onNewLog });

  // A new filter or leaving live mode starts the buffer over.
  const filterKey = JSON.stringify({ ...filters, page: 0 });
  const [bufferKey, setBufferKey] = useState(filterKey + liveMode);
  if (bufferKey !== filterKey + liveMode) {
    setBufferKey(filterKey + liveMode);
    setLiveRows([]);
    setFreshIds(new Set());
  }

  const rows = useMemo(() => {
    const page = data?.content ?? [];
    if (liveRows.length === 0) return page;
    const seen = new Set(liveRows.map((l) => l.id));
    return [...liveRows, ...page.filter((l) => !seen.has(l.id))];
  }, [data, liveRows]);

  // "/" focuses the search box (Instrument keyboard map).
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (e.key !== "/" || target.closest("input, textarea, select, [contenteditable]")) return;
      e.preventDefault();
      document.querySelector<HTMLInputElement>("[role=search] input[type=search]")?.focus();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, []);

  const closeDetail = useCallback(() => setSelectedLog(null), []);

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        action={
          data && (
            <span className="xs t3 mono" aria-live="polite">
              {t("resultCount", { count: data.totalElements ?? rows.length })}
            </span>
          )
        }
      />

      <LogsNav />

      <LogFiltersBar
        filters={filters}
        searchInput={searchInput}
        onFilterChange={updateFilter}
        onReset={resetFilters}
        liveMode={liveMode}
        onToggleLive={toggleLiveMode}
      />

      {liveMode && (
        <p className="row xs t2" aria-live="polite">
          <span className="dot live" aria-hidden="true" />
          {t("liveSummary", { count: liveRows.length })}
          {liveRows.length > 0 && (
            <span className="t3">
              · {t("liveBuffered", { count: liveRows.length, max: LIVE_BUFFER_MAX })}
            </span>
          )}
        </p>
      )}

      <LogTable
        data={rows}
        loading={isLoading}
        onRowClick={setSelectedLog}
        selectedId={selectedLog?.id}
        freshIds={freshIds}
        emptyHint={t("emptyHint")}
      />

      {data && data.totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination
            currentPage={(data.page ?? 0) + 1}
            totalPages={data.totalPages}
            onPageChange={(page) => updateFilter("page", page - 1)}
          />
        </div>
      )}

      <AnimatePresence>
        {selectedLog && <LogDetailPanel key="log-detail" log={selectedLog} onClose={closeDetail} />}
      </AnimatePresence>
    </>
  );
}

export default function LogsPage() {
  // useSearchParams (filters in the URL) needs a Suspense boundary.
  return (
    <Suspense>
      <LogsView />
    </Suspense>
  );
}
