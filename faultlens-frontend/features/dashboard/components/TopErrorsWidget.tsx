"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { TrendingUp } from "lucide-react";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useLogGroupsQuery } from "@/features/logs/api/useLogQuery";

function TopErrorsWidget() {
  const t = useTranslations("dashboard.topErrors");
  const dates = useDateFormat();
  const { data: logGroupsPage, isLoading } = useLogGroupsQuery();
  const topGroups = (logGroupsPage?.content ?? []).slice(0, 5);

  return (
    <section className="card" aria-labelledby="top-errors-title" aria-busy={isLoading || undefined}>
      <div className="panel-h">
        <div>
          <h2 id="top-errors-title" className="h3">
            {t("title")}
          </h2>
          <p className="xs t3">{t("subtitle")}</p>
        </div>
        <TrendingUp size={16} className="t3" aria-hidden="true" />
      </div>

      {isLoading ? (
        <div className="p-4 stack gap-3" aria-hidden="true">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="skel h-9" />
          ))}
        </div>
      ) : topGroups.length === 0 ? (
        <p className="empty">{t("empty")}</p>
      ) : (
        <ol className="m-0 p-0 list-none">
          {topGroups.map((group, index) => (
            <li key={group.id} className={`in d${index + 1}`}>
              <Link
                href={`/logs?group=${group.id}`}
                className="grid grid-cols-[auto_1fr_auto] items-start gap-3 px-4 py-2.5 border-b border-border-default last:border-b-0 hover:bg-bg-glass transition-colors"
              >
                <SeverityBadge severity={group.severity} />
                <div className="min-w-0">
                  <p className="small ellipsis">{group.firstMessage}</p>
                  <p className="xs t3 mt-0.5 row gap-2">
                    <span className="mono ellipsis max-w-[140px]">{group.fingerprint}</span>
                    <span>{t("lastSeen", { time: dates.relative(group.lastSeenAt) })}</span>
                  </p>
                </div>
                <span className="mono small t2 whitespace-nowrap">
                  {t("occurrences", { count: group.count })}
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

export { TopErrorsWidget };
