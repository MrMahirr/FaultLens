"use client";

import { useTranslations } from "next-intl";
import { SeverityBadge, SEVERITY_TONE } from "@/shared/components/ui/SeverityBadge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useSeverityCountsQuery } from "@/features/dashboard/api/useDashboardQuery";

/**
 * Last-hour log counts per severity as labelled bars (the API returns counts for the window,
 * not a time series, so the chart shows exactly that). Each bar has its number and label —
 * colour is never the only cue.
 */
function SeverityChart() {
  const t = useTranslations("dashboard.severity");
  const { number } = useDateFormat();
  const { data, isLoading } = useSeverityCountsQuery();
  const max = Math.max(1, ...(data ?? []).map((d) => d.count));
  const total = (data ?? []).reduce((sum, d) => sum + d.count, 0);

  return (
    <section className="card h-full" aria-labelledby="severity-chart-title">
      <div className="panel-h">
        <div>
          <h2 id="severity-chart-title" className="h3">
            {t("title")}
          </h2>
          <p className="xs t3">{t("subtitle")}</p>
        </div>
        {!isLoading && <span className="num text-[20px]">{number(total)}</span>}
      </div>

      <div className="p-4">
        {isLoading ? (
          <div className="stack gap-3" aria-hidden="true">
            {Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="skel h-5" />
            ))}
          </div>
        ) : total === 0 ? (
          <p className="empty">{t("empty")}</p>
        ) : (
          <ul className="stack gap-2.5 m-0 p-0 list-none">
            {data?.map(({ severity, count }, i) => (
              <li
                key={severity}
                className="grid grid-cols-[96px_1fr_64px] items-center gap-3"
                aria-label={t("count", { severity, count })}
              >
                <SeverityBadge severity={severity} />
                <div className={`bar h-2 ${SEVERITY_TONE[severity]}`} aria-hidden="true">
                  <i
                    style={{
                      width: `${(count / max) * 100}%`,
                      background: "var(--c)",
                      animationDelay: `${i * 18}ms`,
                    }}
                  />
                </div>
                <span className="mono small text-right">{number(count)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

export { SeverityChart };
