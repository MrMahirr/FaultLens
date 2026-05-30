"use client";

import { motion } from "framer-motion";
import { useState } from "react";
import { Brain, RefreshCw, AlertTriangle, Lightbulb, ArrowRight } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";
import { AnalysisType } from "@/shared/types/common.types";
import { formatRelativeTime } from "@/shared/lib/utils";
import { useTriggerAnalysisMutation } from "@/features/analyses/api/useAnalysisMutation";
import { AnalysisDetailModal } from "./AnalysisDetailModal";

/* ── Props ─────────────────────────────────────────────────── */

interface AnalysisCardProps {
  analysis: AnalysisResultDto;
  index: number;
  sourceName?: string;
}

/* ── Component ─────────────────────────────────────────────── */

function AnalysisCard({ analysis, index, sourceName }: AnalysisCardProps) {
  const triggerAnalysis = useTriggerAnalysisMutation();
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: index * 0.1, duration: 0.3 }}
      >
        <Card variant="default" className="border-l-4 border-l-accent hover:border-l-accent-hover transition-colors">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
            <div className="flex items-center gap-3">
              {sourceName && (
                <Badge variant="ghost" size="sm" className="border-border-default text-text-secondary bg-bg-secondary">
                  {sourceName}
                </Badge>
              )}
              <Badge
                variant={analysis.engineType === AnalysisType.RULE_BASED ? "accent" : "info"}
                size="sm"
              >
                {analysis.engineType === AnalysisType.RULE_BASED ? "Kural Tabanlı" : "AI Analizi"}
              </Badge>
              <span className="text-xs text-text-muted">
                {formatRelativeTime(analysis.analyzedAt)}
              </span>
            </div>
            
            <div className="flex items-center gap-2">
              <span className="text-xs text-text-muted">Güven:</span>
              <div className="w-24 h-1.5 bg-bg-tertiary rounded-full">
                <div
                  className="h-full bg-accent rounded-full"
                  style={{ width: `${analysis.confidenceScore * 100}%` }}
                />
              </div>
              <span className="text-xs font-mono text-accent">
                {Math.round(analysis.confidenceScore * 100)}%
              </span>
            </div>
          </div>

          {/* Root Cause Preview */}
          <div className="mb-4">
            <div className="flex items-center gap-2 mb-1">
              <Brain size={14} className="text-accent shrink-0" />
              <p className="text-xs font-medium text-text-muted uppercase tracking-wider">
                Kök Neden Özeti
              </p>
            </div>
            <p className="text-sm text-text-primary line-clamp-2 leading-relaxed">
              {analysis.rootCause}
            </p>
          </div>

          {/* Actions */}
          <div className="flex justify-between items-center mt-2 pt-4 border-t border-border-default/50">
            {analysis.affectedDeployment ? (
              <div className="flex items-center gap-1.5 px-2 py-1 bg-warning/10 rounded text-warning">
                <AlertTriangle size={12} />
                <span className="text-[10px] uppercase font-bold tracking-wide">Deployment Etkilenmiş</span>
              </div>
            ) : <div />}

            <div className="flex gap-2">
              <Button
                variant="ghost"
                size="sm"
                icon={<RefreshCw size={14} />}
                loading={triggerAnalysis.isPending}
                onClick={() => triggerAnalysis.mutate(analysis.logGroupId ?? 0)}
                title="Yeniden Analiz Et"
              />
              <Button
                variant="secondary"
                size="sm"
                icon={<ArrowRight size={14} />}
                iconPosition="right"
                onClick={() => setIsModalOpen(true)}
              >
                Detayları Gör
              </Button>
            </div>
          </div>
        </Card>
      </motion.div>

      <AnalysisDetailModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        analysis={analysis}
        sourceName={sourceName}
      />
    </>
  );
}

export { AnalysisCard };
