"use client";

import { Modal } from "@/shared/components/ui/Modal";
import { Badge } from "@/shared/components/ui/Badge";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";
import { AnalysisType } from "@/shared/types/common.types";
import { formatRelativeTime } from "@/shared/lib/utils";
import { Brain, Lightbulb, AlertTriangle } from "lucide-react";

interface AnalysisDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  analysis: AnalysisResultDto;
  sourceName?: string;
}

export function AnalysisDetailModal({ isOpen, onClose, analysis, sourceName }: AnalysisDetailModalProps) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Analiz Detayı #${analysis.id}`}
      size="lg"
    >
      <div className="space-y-6">
        <div className="flex flex-col gap-2 border-b border-border-default pb-4">
          <div className="flex items-center gap-2">
            {sourceName && (
              <Badge variant="ghost" size="md" className="border-border-default text-text-secondary bg-bg-secondary">
                {sourceName}
              </Badge>
            )}
            <Badge
              variant={analysis.engineType === AnalysisType.RULE_BASED ? "accent" : "info"}
              size="md"
            >
              {analysis.engineType === AnalysisType.RULE_BASED ? "Kural Tabanlı" : "AI Analizi"}
            </Badge>
          </div>
          <span className="text-sm text-text-muted mt-1">
            Analiz Zamanı: {formatRelativeTime(analysis.analyzedAt)}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-text-muted">Güven Skoru:</span>
          <div className="flex-1 h-2 bg-bg-tertiary rounded-full max-w-md">
            <div
              className="h-full bg-accent rounded-full transition-all duration-500"
              style={{ width: `${analysis.confidenceScore * 100}%` }}
            />
          </div>
          <span className="text-sm font-mono text-accent font-bold">
            {Math.round(analysis.confidenceScore * 100)}%
          </span>
        </div>

        <div className="space-y-4">
          <div className="p-4 bg-bg-secondary/50 rounded-xl border border-border-default">
            <div className="flex items-center gap-2 mb-2">
              <Brain size={18} className="text-accent" />
              <h4 className="text-sm font-bold text-text-primary uppercase tracking-wide">Kök Neden</h4>
            </div>
            <p className="text-sm text-text-primary leading-relaxed">
              {analysis.rootCause}
            </p>
          </div>

          <div className="p-4 bg-bg-secondary/50 rounded-xl border border-border-default">
            <div className="flex items-center gap-2 mb-2">
              <Lightbulb size={18} className="text-warning" />
              <h4 className="text-sm font-bold text-text-primary uppercase tracking-wide">Öneri ve Çözüm</h4>
            </div>
            <pre className="text-sm text-text-secondary whitespace-pre-wrap font-body leading-relaxed">
              {analysis.suggestion}
            </pre>
          </div>
        </div>

        {analysis.affectedDeployment && (
          <div className="p-4 rounded-xl bg-warning/5 border border-warning/20 flex items-start gap-3">
            <AlertTriangle size={18} className="text-warning shrink-0 mt-0.5" />
            <div>
              <h4 className="text-sm font-bold text-warning mb-1">Etkilenen Dağıtım (Deployment)</h4>
              <p className="text-sm text-warning/90">
                Bu hata <strong>{analysis.affectedDeployment}</strong> deployment&apos;ı ile ilişkili olabilir.
              </p>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
