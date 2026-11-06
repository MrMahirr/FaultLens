"use client";

import { useTranslations } from "next-intl";
import { Brain, Lightbulb } from "lucide-react";
import { Badge } from "@/shared/components/ui/Badge";
import { AnalysisType } from "@/shared/types/common.types";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";

/** Below this, a result is flagged for human review (Instrument: confidence bar < 0.70). */
export const REVIEW_THRESHOLD = 0.7;

export function EngineBadge({ engine }: { engine: AnalysisType }) {
  const t = useTranslations("analyses.engine");
  return (
    <Badge variant={engine === AnalysisType.AI ? "ai" : "default"} dot>
      {t(engine)}
    </Badge>
  );
}

/**
 * Confidence as a bar + percentage: AI uses --ai, rule-based --fg-2, < 0.70 --warn and an
 * explicit "Review" label (never colour alone).
 */
export function ConfidenceBar({
  score,
  engine,
  className,
}: {
  score: number;
  engine: AnalysisType;
  className?: string;
}) {
  const t = useTranslations("analyses");
  const pct = Math.round(Math.max(0, Math.min(1, score)) * 100);
  const low = score < REVIEW_THRESHOLD;
  const color = low ? "var(--warn)" : engine === AnalysisType.AI ? "var(--ai)" : "var(--fg-2)";

  return (
    <div className={`flex items-center gap-2 ${className ?? ""}`}>
      <span className="xs t3">{t("confidence")}</span>
      <div
        className="bar w-24"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={pct}
        aria-label={t("confidenceValue", { value: pct })}
      >
        <i style={{ width: `${pct}%`, ["--c" as string]: color }} />
      </div>
      <span className="mono xs" style={{ color }}>
        {pct}%
      </span>
      {low && <Badge variant="warning">{t("needsReview")}</Badge>}
    </div>
  );
}

/** Root cause + suggestion blocks of one analysis. */
export function AnalysisBody({
  analysis,
  compact = false,
}: {
  analysis: AnalysisResultDto;
  compact?: boolean;
}) {
  const t = useTranslations("analyses");
  return (
    <div className="stack gap-3">
      <section>
        <h4 className="eyebrow flex items-center gap-1.5 mb-1.5">
          <Brain size={12} aria-hidden="true" />
          {t("rootCause")}
        </h4>
        <p className={compact ? "small" : "text-[14px] leading-relaxed"}>{analysis.rootCause}</p>
      </section>
      {analysis.suggestion && (
        <section>
          <h4 className="eyebrow flex items-center gap-1.5 mb-1.5">
            <Lightbulb size={12} aria-hidden="true" />
            {t("suggestion")}
          </h4>
          <p
            className={`whitespace-pre-wrap t2 ${compact ? "small" : "text-[14px] leading-relaxed"}`}
          >
            {analysis.suggestion}
          </p>
        </section>
      )}
    </div>
  );
}
