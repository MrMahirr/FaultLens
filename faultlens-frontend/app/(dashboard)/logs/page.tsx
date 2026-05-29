"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Pagination } from "@/shared/components/ui/Pagination";
import { LogTable } from "@/features/logs/components/LogTable";
import { LogFiltersBar } from "@/features/logs/components/LogFilters";
import { LogDetailPanel } from "@/features/logs/components/LogDetailPanel";
import { useQueryClient } from "@tanstack/react-query";
import { useLogs as useLogsFilter } from "@/features/logs/hooks/useLogs";
import { useLogsQuery, logKeys } from "@/features/logs/api/useLogQuery";
import { useLogStream } from "@/features/logs/hooks/useLogStream";
import type { LogEntryDto } from "@/features/logs/types/log.types";

export default function LogsPage() {
  const queryClient = useQueryClient();
  const { filters, updateFilter, resetFilters, liveMode, toggleLiveMode } =
    useLogsFilter();
  const { data, isLoading } = useLogsQuery(filters);
  const [selectedLog, setSelectedLog] = useState<LogEntryDto | null>(null);

  // Canlı mod açıksa WebSocket üzerinden logları dinle ve tabloyu güncelle
  useLogStream({
    enabled: liveMode,
    onNewLog: () => {
      // Sadece 1. sayfadaysak sorguyu invalidate et (anında üstten eklensin diye)
      if (filters.page === 0) {
        queryClient.invalidateQueries({ queryKey: logKeys.list(filters) });
      }
    },
  });

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
          <motion.div
            key="log-detail"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 flex justify-end"
          >
            {/* Backdrop */}
            <div
              className="absolute inset-0 bg-black/30"
              onClick={() => setSelectedLog(null)}
            />
            <LogDetailPanel
              log={selectedLog}
              onClose={() => setSelectedLog(null)}
            />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
