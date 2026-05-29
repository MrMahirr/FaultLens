"use client";

import { motion } from "framer-motion";
import { Brain, RefreshCw, AlertTriangle, Lightbulb } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import type { AnalysisResultDto } from "@/shared/mocks/data";
import { AnalysisType } from "@/shared/types/common.types";
import { formatRelativeTime } from "@/shared/lib/utils";
import { useTriggerAnalysis } from "@/features/analyses/api/analyses.queries";

/* ── Props ─────────────────────────────────────────────────── */

interface AnalysisCardProps {
  analysis: AnalysisResultDto;
  index: number;
}

/* ── Component ─────────────────────────────────────────────── */

function AnalysisCard({ analysis, index }: AnalysisCardProps) {
  const triggerMutation = useTriggerAnalysis();

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1, duration: 0.3 }}
    >
      <Card variant="default" className="border-l-4 border-l-accent">
        {/* Header */}
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2">
            <h3 className="font-semibold font-display text-text-primary">
              Analiz #{analysis.id}
            </h3>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Badge
              variant={
                analysis.engineType === AnalysisType.RULE_BASED ? "accent" : "info"
              }
              size="sm"
            >
              {analysis.engineType === AnalysisType.RULE_BASED
                ? "Kural Tabanlı"
                : "AI Analizi"}
            </Badge>
            <span className="text-xs text-text-muted">
              {formatRelativeTime(analysis.analyzedAt)}
            </span>
          </div>
        </div>

        {/* Confidence */}
        <div className="flex items-center gap-2 mb-3">
          <span className="text-xs text-text-muted">Güven:</span>
          <div className="flex-1 h-1.5 bg-bg-tertiary rounded-full max-w-32">
            <div
              className="h-full bg-accent rounded-full transition-all duration-500"
              style={{ width: `${analysis.confidenceScore * 100}%` }}
            />
          </div>
          <span className="text-xs font-mono text-accent">
            {Math.round(analysis.confidenceScore * 100)}%
          </span>
        </div>

        {/* Root Cause */}
        <div className="space-y-3">
          <div className="flex items-start gap-2">
            <Brain size={14} className="text-accent shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                Kök Neden
              </p>
              <p className="text-sm text-text-primary leading-relaxed">
                {analysis.rootCause}
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <Lightbulb size={14} className="text-warning shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-medium text-text-muted uppercase tracking-wider mb-1">
                Öneri
              </p>
              <pre className="text-sm text-text-secondary whitespace-pre-wrap font-body leading-relaxed">
                {analysis.suggestion}
              </pre>
            </div>
          </div>
        </div>

        {/* Affected Deployment */}
        {analysis.affectedDeployment && (
          <div className="mt-3 p-3 rounded-lg bg-warning/5 border border-warning/20 flex items-start gap-2">
            <AlertTriangle size={14} className="text-warning shrink-0 mt-0.5" />
            <p className="text-xs text-warning">
              Bu hata <strong>{analysis.affectedDeployment}</strong> deployment&apos;ı ile ilişkili olabilir.
            </p>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end mt-4">
          <Button
            variant="ghost"
            size="sm"
            icon={<RefreshCw size={14} />}
            loading={triggerMutation.isPending}
            onClick={() => triggerMutation.mutate(analysis.logGroupId ?? 0)}
          >
            Yeniden Analiz Et
          </Button>
        </div>
      </Card>
    </motion.div>
  );
}

export { AnalysisCard };
