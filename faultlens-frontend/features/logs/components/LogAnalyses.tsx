"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { Lightbulb } from "lucide-react";
import { Severity } from "@/shared/types/common.types";
import type { LogEntryDto } from "@/features/logs/types/log.types";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";
import {
  AnalysisBody,
  ConfidenceBar,
  EngineBadge,
} from "@/features/analyses/components/AnalysisParts";

/** Severities the analyzer receives (the processor forwards only these to log-errors). */
const ANALYZED_SEVERITIES = new Set<string>([Severity.ERROR, Severity.CRITICAL]);

/** Root-cause analyses of one log, or why there are none. */
export function LogAnalyses({
  log,
  analyses,
  loading,
}: {
  log: LogEntryDto;
  analyses: AnalysisResultDto[];
  loading: boolean;
}) {
  const t = useTranslations("logs.detail");

  if (loading) {
    return (
      <p className="row xs t3" role="status">
        <span className="spinner" aria-hidden="true" />
        {t("analysesLoading")}
      </p>
    );
  }

  if (analyses.length > 0) {
    return (
      <ul className="stack p-0 m-0 list-none">
        {analyses.map((analysis, i) => (
          <li key={analysis.id || i} className="well p-3 stack gap-3">
            <div className="between">
              <EngineBadge engine={analysis.engineType} />
              <ConfidenceBar score={analysis.confidenceScore} engine={analysis.engineType} />
            </div>
            <AnalysisBody analysis={analysis} compact />
          </li>
        ))}
      </ul>
    );
  }

  if (!ANALYZED_SEVERITIES.has(log.severity)) {
    return <p className="xs t3">{t("notAnalyzed", { severity: log.severity })}</p>;
  }

  if (log.groupId) {
    return (
      <div className="banner c-warn">
        <Lightbulb size={14} aria-hidden="true" />
        <div className="stack gap-2">
          <p className="small">{t("groupHint")}</p>
          <Link href="/analyses" className="link small w-fit">
            {t("goToAnalyses")} →
          </Link>
        </div>
      </div>
    );
  }

  return <p className="xs t3">{t("noAnalysis")}</p>;
}
