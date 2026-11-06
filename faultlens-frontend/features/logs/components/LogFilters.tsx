"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Search, Radio, Pause, X, Trash2, Link2 } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { Button } from "@/shared/components/ui/Button";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { SEVERITY_TONE } from "@/shared/components/ui/SeverityBadge";
import { Severity } from "@/shared/types/common.types";
import { cn } from "@/shared/lib/utils";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import {
  DEFAULT_LOG_TIME_RANGE,
  LOG_TIME_RANGES,
  type LogFilters,
  type LogTimeRange,
} from "@/features/logs/types/log.types";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { useClearLogsMutation } from "@/features/logs/api/useLogMutation";

/* ── Props ─────────────────────────────────────────────────── */

interface LogFiltersBarProps {
  filters: LogFilters;
  /** Current text of the search box (applied to `filters.search` after a pause). */
  searchInput: string;
  onFilterChange: <K extends keyof LogFilters>(key: K, value: LogFilters[K]) => void;
  onReset: () => void;
  liveMode: boolean;
  onToggleLive: () => void;
}

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
  searchInput,
  onFilterChange,
  onReset,
  liveMode,
  onToggleLive,
}: LogFiltersBarProps) {
  const t = useTranslations("logs");
  const dates = useDateFormat();
  const { data: sources } = useSourcesQuery();
  const clearLogs = useClearLogsMutation();
  const canDelete = useCan("delete");
  const { confirm, dialog } = useConfirm();

  const selectedSource = sources?.find((s) => String(s.id) === filters.source);

  const handleClearLogs = () => {
    if (!filters.source) return;
    confirm({
      title: t("clearLogsTitle", { name: selectedSource?.name ?? `#${filters.source}` }),
      description: t("clearLogsDescription"),
      confirmLabel: t("clearLogsConfirm"),
      onConfirm: async () => {
        try {
          await clearLogs.mutateAsync(Number(filters.source));
          onFilterChange("source", undefined);
          toast.success(t("cleared"));
        } catch (error) {
          toast.error(t("clearFailed"));
          throw error;
        }
      },
    });
  };

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      toast.success(t("linkCopied"));
    } catch {
      // Clipboard unavailable (insecure context): the URL bar still holds the link.
    }
  };

  const hasActiveFilters =
    !!filters.startDate ||
    !!filters.groupId ||
    !!filters.severity ||
    !!searchInput ||
    !!filters.source ||
    (filters.range ?? DEFAULT_LOG_TIME_RANGE) !== DEFAULT_LOG_TIME_RANGE;

  return (
    <div className="card p-3 stack gap-3" role="search" aria-label={t("search")}>
      <div className="flex flex-wrap items-center gap-2">
        <label className="search grow">
          <Search size={15} aria-hidden="true" />
          <span className="sr-only">{t("search")}</span>
          <input
            type="search"
            placeholder={t("searchPlaceholder")}
            value={searchInput}
            onChange={(e) => onFilterChange("search", e.target.value || undefined)}
          />
          <kbd className="kbd" aria-hidden="true">
            /
          </kbd>
        </label>

        <label className="sr-only" htmlFor="log-range">
          {t("range")}
        </label>
        <select
          id="log-range"
          className="input w-auto"
          value={filters.range ?? DEFAULT_LOG_TIME_RANGE}
          onChange={(e) => onFilterChange("range", e.target.value as LogTimeRange)}
        >
          {LOG_TIME_RANGES.map((range) => (
            <option key={range} value={range}>
              {t(`ranges.${range}`)}
            </option>
          ))}
        </select>

        <label className="sr-only" htmlFor="log-source">
          {t("source")}
        </label>
        <select
          id="log-source"
          className="input w-auto max-w-[220px]"
          value={filters.source ?? ""}
          onChange={(e) => onFilterChange("source", e.target.value || undefined)}
        >
          <option value="">{t("allSources")}</option>
          {sources?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} ({s.type})
            </option>
          ))}
        </select>

        <Button
          variant={liveMode ? "primary" : "secondary"}
          icon={
            liveMode ? (
              <Pause size={14} aria-hidden="true" />
            ) : (
              <Radio size={14} aria-hidden="true" />
            )
          }
          onClick={onToggleLive}
          aria-pressed={liveMode}
        >
          {liveMode ? t("pause") : t("resume")}
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="xs t3 mr-1" id="severity-filter-label">
          {t("severity")}
        </span>
        <div className="row gap-1.5" role="group" aria-labelledby="severity-filter-label">
          {SEVERITY_OPTIONS.map((severity) => {
            const active = filters.severity === severity;
            return (
              <button
                key={severity}
                type="button"
                aria-pressed={active}
                onClick={() => onFilterChange("severity", active ? undefined : severity)}
                // Inactive chips use --fg-3 (not Instrument's --fg-4) to keep text at ≥ 4.5:1.
                className={cn("chip mono", SEVERITY_TONE[severity], !active && "text-text-muted")}
                style={
                  active
                    ? {
                        color: "var(--c)",
                        borderColor: "color-mix(in srgb, var(--c) 45%, transparent)",
                        background: "color-mix(in srgb, var(--c) 13%, transparent)",
                      }
                    : undefined
                }
              >
                {active && <span className="d" aria-hidden="true" />}
                {severity}
              </button>
            );
          })}
        </div>

        {filters.groupId && (
          <button
            type="button"
            className="chip mono"
            aria-pressed="true"
            onClick={() => onFilterChange("groupId", undefined)}
            aria-label={t("removeGroupFilter")}
          >
            {t("groupFilter", { id: filters.groupId })}
            <X size={12} aria-hidden="true" />
          </button>
        )}

        {filters.startDate && (
          <button
            type="button"
            className="chip"
            aria-pressed="true"
            onClick={() => onFilterChange("startDate", undefined)}
            aria-label={t("removeSinceFilter")}
          >
            {t("sinceFilter", { time: dates.dateTime(filters.startDate) })}
            <X size={12} aria-hidden="true" />
          </button>
        )}

        <div className="row ml-auto">
          {hasActiveFilters && (
            <>
              <Button
                variant="ghost"
                size="sm"
                icon={<Link2 size={13} aria-hidden="true" />}
                onClick={copyLink}
              >
                {t("copyLink")}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                icon={<X size={13} aria-hidden="true" />}
                onClick={onReset}
              >
                {t("clear")}
              </Button>
            </>
          )}
          {filters.source && canDelete && (
            <Button
              variant="danger"
              size="sm"
              icon={<Trash2 size={13} aria-hidden="true" />}
              loading={clearLogs.isPending}
              onClick={handleClearLogs}
            >
              {t("clearLogs")}
            </Button>
          )}
        </div>
      </div>
      {dialog}
    </div>
  );
}

export { LogFiltersBar };
