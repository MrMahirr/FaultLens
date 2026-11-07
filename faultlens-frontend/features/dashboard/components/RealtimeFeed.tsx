"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useLogsQuery } from "@/features/logs/api/useLogQuery";
import { useLogStream } from "@/features/logs/hooks/useLogStream";
import type { LogEntryDto } from "@/features/logs/types/log.types";
import { Severity } from "@/shared/types/common.types";
import { cn } from "@/shared/lib/utils";

const MAX_FEED_ITEMS = 30; // cap to keep memory bounded on a busy stream

function RealtimeFeed() {
  const t = useTranslations("dashboard.feed");
  const tLogs = useTranslations("logs");
  const dates = useDateFormat();
  // Only logs received over the WebSocket live in state; the initial page comes from the
  // query and is merged at render time (no state syncing in an effect).
  const [liveLogs, setLiveLogs] = useState<LogEntryDto[]>([]);
  const [freshIds, setFreshIds] = useState<ReadonlySet<number>>(new Set());

  const { data: initialData } = useLogsQuery({ page: 0, size: 20, severity: Severity.ERROR });

  const { isConnected } = useLogStream({
    enabled: true,
    onNewLog: (newLog) => {
      if (newLog.severity !== Severity.ERROR && newLog.severity !== Severity.CRITICAL) return;
      setLiveLogs((prev) =>
        prev.some((l) => l.id === newLog.id) ? prev : [newLog, ...prev].slice(0, MAX_FEED_ITEMS),
      );
      setFreshIds((prev) => new Set(prev).add(newLog.id));
    },
  });

  const logs = useMemo(() => {
    const seen = new Set(liveLogs.map((l) => l.id));
    const initial = (initialData?.content ?? []).filter((l) => !seen.has(l.id));
    return [...liveLogs, ...initial].slice(0, MAX_FEED_ITEMS);
  }, [liveLogs, initialData]);

  return (
    <section className="card h-full flex flex-col" aria-labelledby="feed-title">
      <div className="panel-h">
        <div>
          <h2 id="feed-title" className="h3">
            {t("title")}
          </h2>
          <p className="xs t3">{t("subtitle")}</p>
        </div>
        <span className={cn("row xs", isConnected ? "text-success" : "t3")}>
          <span
            className={cn("dot", isConnected && "live")}
            style={isConnected ? undefined : { background: "var(--fg-4)" }}
            aria-hidden="true"
          />
          {isConnected ? t("live") : t("offline")}
        </span>
      </div>

      <ul
        className="flex-1 max-h-[360px] overflow-y-auto m-0 p-0 list-none"
        aria-label={t("title")}
      >
        {logs.map((log) => (
          <li key={log.id} className={cn(freshIds.has(log.id) && "inA")}>
            <Link
              href={`/logs/${log.id}`}
              className="flex items-start gap-3 px-4 py-2.5 border-b border-border-default hover:bg-bg-glass transition-colors"
            >
              <SeverityBadge severity={log.severity} />
              <div className="flex-1 min-w-0">
                <p className="small ellipsis">{log.message}</p>
                <p className="xs t3 mt-0.5 row gap-2">
                  <span className="mono ellipsis max-w-[160px]">
                    {log.podName ??
                      log.serviceName ??
                      tLogs("sourceFallback", { id: log.sourceId })}
                  </span>
                  <time dateTime={log.timestamp}>{dates.relative(log.timestamp)}</time>
                </p>
              </div>
            </Link>
          </li>
        ))}
        {logs.length === 0 && <li className="empty">{t("empty")}</li>}
      </ul>
    </section>
  );
}

export { RealtimeFeed };
