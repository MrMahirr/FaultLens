"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ArrowLeft, FileQuestion } from "lucide-react";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { LoadingSpinner } from "@/shared/components/feedback/LoadingSpinner";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useLogDetailQuery } from "@/features/logs/api/useLogQuery";
import { StackTrace } from "@/features/logs/components/StackTrace";
import { LogAnalyses } from "@/features/logs/components/LogAnalyses";

export default function LogDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const t = useTranslations("logs.detail");
  const dates = useDateFormat();
  const { id } = use(params);
  const logId = parseInt(id, 10);
  const { data: detailData, isLoading, error } = useLogDetailQuery(logId);
  const log = detailData?.log;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <LoadingSpinner label={t("loading")} />
      </div>
    );
  }

  if (error || !log) {
    return (
      <div className="card">
        <EmptyState icon={<FileQuestion size={18} />} title={t("notFound")} />
        <div className="flex justify-center pb-8">
          <Link href="/logs" className="btn btn-sm">
            <ArrowLeft size={14} aria-hidden="true" />
            {t("back")}
          </Link>
        </div>
      </div>
    );
  }

  const facts: [string, string | number | undefined][] = [
    [t("service"), log.serviceName],
    [t("sourceId"), log.sourceId],
    [t("namespace"), log.namespace],
    [t("pod"), log.podName],
    [t("container"), log.containerName],
  ];

  return (
    <div className="stack gap-5 max-w-4xl">
      <Link href="/logs" className="btn btn-ghost btn-sm w-fit -ml-2">
        <ArrowLeft size={14} aria-hidden="true" />
        {t("back")}
      </Link>

      <header className="page-head">
        <div className="stack gap-2">
          <h1 className="h1 mono">{t("logNumber", { id: log.id })}</h1>
          <div className="row">
            <SeverityBadge severity={log.severity} />
            <time dateTime={log.timestamp} className="small t3 mono">
              {dates.dateTimeLong(log.timestamp)}
            </time>
          </div>
        </div>
      </header>

      <section className="card p-4 field">
        <h2 className="eyebrow">{t("message")}</h2>
        <p className="text-[15px] leading-relaxed break-words">{log.message}</p>
      </section>

      <dl className="grid grid-cols-2 lg:grid-cols-5 gap-3 m-0">
        {facts.map(([label, value]) => (
          <div key={label} className="card p-3">
            <dt className="eyebrow">{label}</dt>
            <dd className="mono small mt-1 m-0 break-all">{value ?? "—"}</dd>
          </div>
        ))}
      </dl>

      {log.stackTrace && (
        <section className="field">
          <h2 className="eyebrow">{t("stackTrace")}</h2>
          <StackTrace trace={log.stackTrace} />
        </section>
      )}

      <section className="field">
        <h2 className="eyebrow">{t("analysis")}</h2>
        <LogAnalyses log={log} analyses={detailData?.analyses ?? []} loading={false} />
      </section>
    </div>
  );
}
