"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { Sparkles, ArrowRight } from "lucide-react";
import { useAnalysesQuery } from "@/features/analyses/api/useAnalysisQuery";
import { AnalysisType } from "@/shared/types/common.types";

function AiSummaryWidget() {
  const t = useTranslations("dashboard.summary");
  const { data: analysisData, isLoading } = useAnalysesQuery({ page: 0, size: 1 });

  const totalElements = analysisData?.totalElements ?? 0;
  const latest = analysisData?.content?.[0];

  return (
    <section
      className="card p-4 flex flex-col md:flex-row md:items-center justify-between gap-4"
      style={{ boxShadow: "inset 2px 0 0 var(--ai)" }}
      aria-labelledby="ai-summary-title"
    >
      <div className="flex items-start gap-3">
        <span
          className="w-8 h-8 rounded-md well grid place-items-center text-ai shrink-0"
          aria-hidden="true"
        >
          <Sparkles size={16} />
        </span>
        <div className="min-w-0">
          <h2 id="ai-summary-title" className="h3 mb-1">
            {t("title")}
          </h2>
          {isLoading ? (
            <div className="stack gap-2 mt-1" aria-hidden="true">
              <div className="skel h-3.5 w-72 max-w-full" />
              <div className="skel h-3.5 w-52 max-w-full" />
            </div>
          ) : totalElements > 0 ? (
            <p className="small t2 max-w-3xl">
              {t("total", { count: totalElements })}{" "}
              {latest &&
                (latest.affectedDeployment
                  ? t("latestDeployment", { deployment: latest.affectedDeployment })
                  : t("latestGeneral"))}{" "}
              {latest && (latest.engineType === AnalysisType.AI ? t("engineAi") : t("engineRule"))}
            </p>
          ) : (
            <p className="small t2 max-w-3xl">{t("none")}</p>
          )}
        </div>
      </div>

      <Link href="/analyses" className="btn shrink-0">
        {totalElements > 0 ? t("open") : t("goTo")}
        <ArrowRight size={14} aria-hidden="true" />
      </Link>
    </section>
  );
}

export { AiSummaryWidget };
