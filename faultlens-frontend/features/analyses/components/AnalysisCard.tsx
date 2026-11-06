"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { RefreshCw, AlertTriangle, ArrowRight } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";
import { useTriggerAnalysisMutation } from "@/features/analyses/api/useAnalysisMutation";
import { AnalysisDetailModal } from "./AnalysisDetailModal";
import { ConfidenceBar, EngineBadge } from "./AnalysisParts";

/* ── Props ─────────────────────────────────────────────────── */

interface AnalysisCardProps {
  analysis: AnalysisResultDto;
  /** Position in the list (used for the staggered entry animation). */
  index: number;
  sourceName?: string;
}

/* ── Component ─────────────────────────────────────────────── */

function AnalysisCard({ analysis, index, sourceName }: AnalysisCardProps) {
  const t = useTranslations("analyses");
  const dates = useDateFormat();
  const triggerAnalysis = useTriggerAnalysisMutation();
  const canEdit = useCan("edit");
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <li className={`card hov in d${Math.min(index + 1, 8)} list-none`}>
      <article className="p-4 stack gap-3" aria-labelledby={`analysis-${analysis.id}`}>
        <header className="between">
          <div className="row">
            <EngineBadge engine={analysis.engineType} />
            {sourceName && <Badge variant="outline">{sourceName}</Badge>}
            <time
              dateTime={analysis.analyzedAt}
              className="xs t3"
              title={dates.dateTime(analysis.analyzedAt)}
            >
              {dates.relative(analysis.analyzedAt)}
            </time>
          </div>
          <ConfidenceBar score={analysis.confidenceScore} engine={analysis.engineType} />
        </header>

        <div>
          <h3 id={`analysis-${analysis.id}`} className="eyebrow mb-1">
            {t("rootCauseSummary")}
          </h3>
          <p className="text-[14px] line-clamp-2 leading-relaxed">{analysis.rootCause}</p>
        </div>

        <footer className="between pt-3 border-t border-border-default">
          {analysis.affectedDeployment ? (
            <Badge variant="warning" icon={<AlertTriangle size={12} aria-hidden="true" />}>
              {t("affectedDeployment")}
            </Badge>
          ) : (
            <span />
          )}

          <div className="row">
            {canEdit && analysis.logGroupId && (
              <Button
                variant="ghost"
                size="sm"
                icon={<RefreshCw size={14} aria-hidden="true" />}
                loading={triggerAnalysis.isPending}
                onClick={() => triggerAnalysis.mutate(analysis.logGroupId ?? 0)}
                title={t("reanalyze")}
                aria-label={t("reanalyze")}
              />
            )}
            <Button
              variant="secondary"
              size="sm"
              icon={<ArrowRight size={14} aria-hidden="true" />}
              iconPosition="right"
              onClick={() => setIsModalOpen(true)}
            >
              {t("details")}
            </Button>
          </div>
        </footer>
      </article>

      <AnalysisDetailModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        analysis={analysis}
        sourceName={sourceName}
      />
    </li>
  );
}

export { AnalysisCard };
