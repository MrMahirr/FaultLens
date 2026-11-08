"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronDown, ChevronUp } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { getApiErrorMessage } from "@/shared/api/errors";
import { cn } from "@/shared/lib/utils";
import { LogApi } from "@/features/logs/api/logApi";
import { useLogContextQuery } from "@/features/logs/api/useLogQuery";
import type { LogContextDto, LogEntryDto } from "@/features/logs/types/log.types";

const PAGE = 50;

/** Pages loaded with "load more", on top of the first ±50 lines from the query. */
interface Extra {
  anchorId: number;
  before: LogEntryDto[];
  after: LogEntryDto[];
  hasMoreBefore?: boolean;
  hasMoreAfter?: boolean;
}

/**
 * What the same source and pod logged around one entry (P10 / 2.3): ±50 lines, oldest first,
 * the entry itself highlighted and scrolled to the middle; more lines load from either edge.
 * A compact list (time · severity · message) — it also fits the 520 px drawer.
 */
export function LogContext({
  log,
  onSelect,
  className,
}: {
  log: Pick<LogEntryDto, "id" | "podName">;
  /** Opens another line of the context (drawer: shows it; page: navigates). */
  onSelect?: (entry: LogEntryDto) => void;
  className?: string;
}) {
  const t = useTranslations("logs.context");
  const dates = useDateFormat();
  const { data, isLoading, error } = useLogContextQuery(log.id, PAGE);
  // Extra pages belong to one anchor; switching entries starts from the query's page again.
  const [loaded, setLoaded] = useState<Extra>({ anchorId: log.id, before: [], after: [] });
  const extra = loaded.anchorId === log.id ? loaded : { anchorId: log.id, before: [], after: [] };
  const [loading, setLoading] = useState<"before" | "after" | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const listRef = useRef<HTMLOListElement>(null);

  const entries = data ? [...extra.before, ...data.entries, ...extra.after] : [];
  const hasMoreBefore = extra.hasMoreBefore ?? data?.hasMoreBefore ?? false;
  const hasMoreAfter = extra.hasMoreAfter ?? data?.hasMoreAfter ?? false;

  // Bring the entry itself to the middle once its context is there.
  useEffect(() => {
    if (!data) return;
    listRef.current
      ?.querySelector<HTMLElement>(`[data-anchor="true"]`)
      ?.scrollIntoView({ block: "center" });
  }, [data]);

  const loadMore = async (side: "before" | "after") => {
    const edge = side === "before" ? entries[0] : entries[entries.length - 1];
    if (!edge) return;
    setLoading(side);
    setLoadError(null);
    try {
      const page: LogContextDto = await LogApi.getLogContext(
        edge.id,
        side === "before" ? PAGE : 0,
        side === "after" ? PAGE : 0,
      );
      // The page includes its anchor (the edge entry we already show).
      const fresh = page.entries.filter((entry) => entry.id !== edge.id);
      setLoaded({
        ...extra,
        ...(side === "before"
          ? { before: [...fresh, ...extra.before], hasMoreBefore: page.hasMoreBefore }
          : { after: [...extra.after, ...fresh], hasMoreAfter: page.hasMoreAfter }),
      });
    } catch (e) {
      setLoadError(getApiErrorMessage(e, t("failed")));
    } finally {
      setLoading(null);
    }
  };

  if (isLoading) {
    return (
      <p className="small t3" aria-busy="true">
        {t("loading")}
      </p>
    );
  }
  if (error || !data) {
    return <p className="small text-critical">{getApiErrorMessage(error, t("failed"))}</p>;
  }

  return (
    <div className={cn("stack gap-2", className)}>
      <p className="xs t3">
        {log.podName
          ? t("scopePod", { pod: log.podName, minutes: data.windowMinutes })
          : t("scopeSource", { minutes: data.windowMinutes })}
      </p>
      {hasMoreBefore && (
        <Button
          size="sm"
          variant="ghost"
          className="w-fit"
          icon={<ChevronUp size={13} aria-hidden="true" />}
          loading={loading === "before"}
          onClick={() => void loadMore("before")}
        >
          {t("loadBefore")}
        </Button>
      )}
      <ol
        ref={listRef}
        className="card m-0 p-0 list-none overflow-auto max-h-[60vh] divide-y divide-border-default"
        aria-label={t("label")}
      >
        {entries.map((entry) => {
          const anchor = entry.id === data.anchorId;
          return (
            <li key={entry.id} data-anchor={anchor || undefined}>
              <button
                type="button"
                className={cn(
                  "w-full text-left grid grid-cols-[72px_78px_minmax(0,1fr)] gap-2 items-start px-3 py-1.5 hover:bg-[var(--hover)]",
                  anchor && "bg-[var(--active)]",
                )}
                style={anchor ? { boxShadow: "inset 2px 0 0 var(--signal)" } : undefined}
                aria-current={anchor ? "true" : undefined}
                onClick={() => onSelect?.(entry)}
                disabled={!onSelect || anchor}
              >
                <time dateTime={entry.timestamp} className="xs mono t3 pt-0.5">
                  {dates.time(entry.timestamp)}
                </time>
                <SeverityBadge severity={entry.severity} />
                <span className="small mono break-words line-clamp-3">{entry.message}</span>
              </button>
            </li>
          );
        })}
      </ol>
      {hasMoreAfter && (
        <Button
          size="sm"
          variant="ghost"
          className="w-fit"
          icon={<ChevronDown size={13} aria-hidden="true" />}
          loading={loading === "after"}
          onClick={() => void loadMore("after")}
        >
          {t("loadAfter")}
        </Button>
      )}
      {entries.length === 1 && <p className="xs t3">{t("alone")}</p>}
      {loadError && (
        <p className="xs text-critical" role="alert">
          {loadError}
        </p>
      )}
    </div>
  );
}
