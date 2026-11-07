"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { ConnectionStatus } from "@/shared/types/common.types";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { cn } from "@/shared/lib/utils";

const STATUS_TONE: Record<ConnectionStatus, string> = {
  [ConnectionStatus.CONNECTED]: "c-ok",
  [ConnectionStatus.WARNING]: "c-warn",
  [ConnectionStatus.DISCONNECTED]: "c-crit",
};

function SystemHealthMap() {
  const t = useTranslations("dashboard.health");
  const tSources = useTranslations("sources");
  const dates = useDateFormat();
  const { data: sources, isLoading } = useSourcesQuery();

  return (
    <section className="card" aria-labelledby="health-title">
      <div className="panel-h">
        <div>
          <h2 id="health-title" className="h3">
            {t("title")}
          </h2>
          <p className="xs t3">{t("subtitle")}</p>
        </div>
        <Link href="/sources" className="link xs">
          {t("viewAll")}
        </Link>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4" aria-hidden="true">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="skel h-[68px]" />
          ))}
        </div>
      ) : !sources || sources.length === 0 ? (
        <div className="empty">
          <p>{t("empty")}</p>
          <Link href="/sources" className="btn btn-sm">
            {t("addSource")}
          </Link>
        </div>
      ) : (
        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4 m-0 list-none">
          {sources.slice(0, 6).map((source) => {
            const status = source.status ?? ConnectionStatus.DISCONNECTED;
            return (
              <li key={source.id} className="well p-3 min-w-0">
                <div className="row gap-2 mb-1.5">
                  <span
                    className={cn(
                      "dot",
                      STATUS_TONE[status],
                      status === ConnectionStatus.CONNECTED && "live",
                    )}
                    aria-hidden="true"
                  />
                  <span className="small font-medium ellipsis">{source.name}</span>
                </div>
                <p className="xs t3">
                  {tSources(`types.${source.type}`)} · {t(`status.${status}`)}
                </p>
                <p className="xs t3">{t("lastLog", { time: dates.relative(source.lastSeenAt) })}</p>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

export { SystemHealthMap };
