"use client";

import { useState } from "react";
import { AnimatePresence } from "framer-motion";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Pagination } from "@/shared/components/ui/Pagination";
import { LogTable } from "@/features/logs/components/LogTable";
import { LogFiltersBar } from "@/features/logs/components/LogFilters";
import { LogDetailPanel } from "@/features/logs/components/LogDetailPanel";
import { useLogs as useLogsFilter } from "@/features/logs/hooks/useLogs";
import { useLogs as useLogsQuery } from "@/features/logs/api/logs.queries";
import type { LogEntryDto } from "@/features/logs/types/log.types";

export default function LogsPage() {
  const { filters, updateFilter, resetFilters, liveMode, toggleLiveMode } =
    useLogsFilter();
  const { data, isLoading } = useLogsQuery(filters);
  const [selectedLog, setSelectedLog] = useState<LogEntryDto | null>(null);

  return (
    <div className="space-y-4">
      <PageHeader
        title="Logs"
        description="Tüm kaynaklardan gelen logları izleyin ve filtreleyin"
      />

      {/* Filters */}
      <LogFiltersBar
        filters={filters}
        onFilterChange={updateFilter}
        onReset={resetFilters}
        liveMode={liveMode}
        onToggleLive={toggleLiveMode}
      />

      {/* Table */}
      <LogTable
        data={data?.content ?? []}
        loading={isLoading}
        onRowClick={setSelectedLog}
      />

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center mt-4">
          <Pagination
            currentPage={(data.page ?? 0) + 1}
            totalPages={data.totalPages}
            onPageChange={(page) => updateFilter("page", page - 1)}
          />
        </div>
      )}

      {/* Detail Panel */}
      <AnimatePresence>
        {selectedLog && (
          <>
            {/* Backdrop */}
            <div
              className="fixed inset-0 bg-black/30 z-40"
              onClick={() => setSelectedLog(null)}
            />
            <LogDetailPanel
              log={selectedLog}
              onClose={() => setSelectedLog(null)}
            />
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
