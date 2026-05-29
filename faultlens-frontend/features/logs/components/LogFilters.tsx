"use client";

import { Search, Filter, Radio, X, Trash2 } from "lucide-react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { Badge } from "@/shared/components/ui/Badge";
import { Severity } from "@/shared/types/common.types";
import type { LogFilters } from "@/features/logs/types/log.types";
import { cn } from "@/shared/lib/utils";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { useClearLogsMutation } from "@/features/logs/api/useLogMutation";

/* ── Props ─────────────────────────────────────────────────── */

interface LogFiltersBarProps {
  filters: LogFilters;
  onFilterChange: <K extends keyof LogFilters>(
    key: K,
    value: LogFilters[K]
  ) => void;
  onReset: () => void;
  liveMode: boolean;
  onToggleLive: () => void;
}

/* ── Severity Options ──────────────────────────────────────── */

const SEVERITY_OPTIONS = [
  Severity.CRITICAL,
  Severity.ERROR,
  Severity.WARN,
  Severity.INFO,
  Severity.DEBUG,
  Severity.TRACE,
];

/* ── Component ─────────────────────────────────────────────── */

function LogFiltersBar({
  filters,
  onFilterChange,
  onReset,
  liveMode,
  onToggleLive,
}: LogFiltersBarProps) {
  const { data: sources } = useSourcesQuery();
  const clearLogs = useClearLogsMutation();

  const handleClearLogs = async () => {
    if (!filters.source) return;
    const confirmDelete = window.confirm("Bu projeye ait tüm logları ve analizleri kalıcı olarak silmek istediğinize emin misiniz?");
    if (confirmDelete) {
      await clearLogs.mutateAsync(Number(filters.source));
      onFilterChange("source", undefined);
    }
  };
  const toggleSeverity = (severity: Severity) => {
    const current = filters.severity ?? [];
    const updated = current.includes(severity)
      ? current.filter((s) => s !== severity)
      : [...current, severity];
    onFilterChange("severity", updated.length > 0 ? updated : undefined);
  };

  const hasActiveFilters =
    (filters.severity && filters.severity.length > 0) ||
    filters.search ||
    filters.source;

  return (
    <div className="flex flex-col gap-3 p-4 bg-bg-secondary border border-border-default rounded-xl">
      {/* Top Row: Search + Live Mode */}
      <div className="flex items-center gap-3">
        <div className="flex-1">
          <Input
            placeholder="Log ara..."
            icon={<Search size={16} />}
            value={filters.search ?? ""}
            onChange={(e) => onFilterChange("search", e.target.value || undefined)}
          />
        </div>

        <Button
          variant={liveMode ? "success" : "secondary"}
          size="md"
          icon={<Radio size={14} className={liveMode ? "animate-pulse" : ""} />}
          onClick={onToggleLive}
        >
          {liveMode ? "Canlı" : "Canlı Mod"}
        </Button>
      </div>

      {/* Severity Filters */}
      <div className="flex items-center gap-2 flex-wrap">
        <Filter size={14} className="text-text-muted shrink-0" />

        {SEVERITY_OPTIONS.map((severity) => {
          const isActive = filters.severity?.includes(severity);
          return (
            <button
              key={severity}
              onClick={() => toggleSeverity(severity)}
              className={cn(
                "px-2.5 py-1 rounded-full text-xs font-mono font-medium",
                "transition-all duration-200 border",
                isActive
                  ? `severity-${severity.toLowerCase()} border-current`
                  : "text-text-muted border-border-default hover:border-border-hover"
              )}
            >
              {severity}
            </button>
          );
        })}

        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="sm"
            icon={<X size={12} />}
            onClick={onReset}
          >
            Temizle
          </Button>
        )}
        {/* Source Dropdown & Clear Logs Button */}
        <div className="flex items-center gap-2 ml-auto">
          <select
            className="px-3 py-1.5 bg-bg-primary border border-border-default rounded-lg text-sm text-text-primary focus:outline-none focus:border-accent"
            value={filters.source ?? ""}
            onChange={(e) => onFilterChange("source", e.target.value || undefined)}
          >
            <option value="">Tüm Projeler</option>
            {sources?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.type})
              </option>
            ))}
          </select>

          {filters.source && (
            <Button
              variant="error"
              size="sm"
              icon={<Trash2 size={14} />}
              loading={clearLogs.isPending}
              onClick={handleClearLogs}
            >
              Logları Temizle
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

export { LogFiltersBar };
