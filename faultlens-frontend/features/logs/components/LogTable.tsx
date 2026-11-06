"use client";

import { useRef, type KeyboardEvent } from "react";
import { useVirtualizer } from "@tanstack/react-virtual";
import { useTranslations } from "next-intl";
import { SearchX } from "lucide-react";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useUIStore } from "@/shared/store/ui.store";
import { cn } from "@/shared/lib/utils";
import type { LogEntryDto } from "@/features/logs/types/log.types";

/* ── Props ─────────────────────────────────────────────────── */

interface LogTableProps {
  data: LogEntryDto[];
  loading?: boolean;
  onRowClick?: (log: LogEntryDto) => void;
  /** Id of the row whose detail is open (highlighted, aria-current). */
  selectedId?: number;
  /** Ids that just arrived over the live stream (entry animation). */
  freshIds?: ReadonlySet<number>;
  /** Shown in the empty state ("widen the filter…"). */
  emptyHint?: string;
  className?: string;
}

/** time · severity · source · message · namespace · group */
const GRID = "grid-cols-[96px_100px_minmax(120px,180px)_minmax(240px,1fr)_120px_64px]";
const OVERSCAN = 12;

/* ── Component ─────────────────────────────────────────────── */

/**
 * Virtualised log list (TanStack Virtual): only the visible rows are in the DOM, so the live
 * stream can hold thousands of entries. ARIA table roles keep it readable as a table; rows
 * open with click or Enter, ↑/↓ move between rows.
 */
function LogTable({
  data,
  loading = false,
  onRowClick,
  selectedId,
  freshIds,
  emptyHint,
  className,
}: LogTableProps) {
  const t = useTranslations("logs");
  const dates = useDateFormat();
  const compact = useUIStore((state) => state.compactMode);
  const rowHeight = compact ? 32 : 40;
  const scrollRef = useRef<HTMLDivElement>(null);

  // TanStack Virtual returns a mutable instance; the React Compiler skips this component.
  // eslint-disable-next-line react-hooks/incompatible-library
  const virtualizer = useVirtualizer({
    count: data.length,
    getScrollElement: () => scrollRef.current,
    estimateSize: () => rowHeight,
    overscan: OVERSCAN,
    getItemKey: (index) => data[index]?.id ?? index,
  });

  const focusRow = (index: number) => {
    if (index < 0 || index >= data.length) return;
    virtualizer.scrollToIndex(index, { align: "auto" });
    requestAnimationFrame(() =>
      scrollRef.current?.querySelector<HTMLElement>(`[data-index="${index}"]`)?.focus(),
    );
  };

  const onRowKeyDown = (e: KeyboardEvent, index: number, log: LogEntryDto) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onRowClick?.(log);
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      focusRow(index + 1);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      focusRow(index - 1);
    }
  };

  const header = (
    <div
      role="row"
      className={cn("tr th grid sticky top-0 z-10 border-b border-border-default", GRID)}
    >
      {(["time", "severity", "source", "message", "namespace", "group"] as const).map((col) => (
        <span key={col} role="columnheader">
          {t(`columns.${col}`)}
        </span>
      ))}
    </div>
  );

  return (
    <div className={cn("card overflow-hidden", className)}>
      <div
        ref={scrollRef}
        role="table"
        aria-label={t("table")}
        aria-rowcount={data.length + 1}
        aria-busy={loading || undefined}
        className="overflow-auto h-[calc(100vh-330px)] min-h-[360px]"
      >
        <div className="min-w-[860px]">
          <div role="rowgroup">{header}</div>

          {loading && data.length === 0 ? (
            <div role="rowgroup" aria-hidden="true">
              {Array.from({ length: 10 }, (_, i) => (
                <div key={i} className={cn("tr grid", GRID)} style={{ minHeight: rowHeight }}>
                  {Array.from({ length: 6 }, (_, j) => (
                    <span key={j} className="skel h-3" />
                  ))}
                </div>
              ))}
            </div>
          ) : data.length === 0 ? (
            <EmptyState icon={<SearchX size={18} />} title={t("empty")} description={emptyHint} />
          ) : (
            <div
              role="rowgroup"
              className="relative"
              style={{ height: virtualizer.getTotalSize() }}
            >
              {virtualizer.getVirtualItems().map((item) => {
                const log = data[item.index];
                const selected = log.id === selectedId;
                return (
                  <div
                    key={item.key}
                    role="row"
                    data-index={item.index}
                    aria-rowindex={item.index + 2}
                    aria-current={selected || undefined}
                    tabIndex={0}
                    onClick={() => onRowClick?.(log)}
                    onKeyDown={(e) => onRowKeyDown(e, item.index, log)}
                    className={cn(
                      "tr grid absolute left-0 cursor-pointer outline-offset-[-2px]",
                      GRID,
                      freshIds?.has(log.id) && "inA",
                    )}
                    style={{
                      transform: `translateY(${item.start}px)`,
                      height: rowHeight,
                      minHeight: rowHeight,
                    }}
                  >
                    <span role="cell" className="mono text-text-muted whitespace-nowrap">
                      <time dateTime={log.timestamp} title={dates.dateTimeLong(log.timestamp)}>
                        {dates.time(log.timestamp)}
                      </time>
                    </span>
                    <span role="cell">
                      <SeverityBadge severity={log.severity} />
                    </span>
                    <span role="cell" className="mono text-text-secondary ellipsis">
                      {log.podName ?? log.serviceName ?? t("sourceFallback", { id: log.sourceId })}
                    </span>
                    <span role="cell" className="ellipsis" title={log.message}>
                      {log.message}
                    </span>
                    <span role="cell" className="xs t3 ellipsis">
                      {log.namespace ?? "—"}
                    </span>
                    <span role="cell" className="mono xs">
                      {log.groupId ? (
                        <span className="text-accent">#{log.groupId}</span>
                      ) : (
                        <span className="t4" aria-label="—">
                          —
                        </span>
                      )}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export { LogTable };
