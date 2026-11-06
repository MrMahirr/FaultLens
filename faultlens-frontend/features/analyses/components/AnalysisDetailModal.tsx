"use client";

import { useTranslations } from "next-intl";
import { AlertTriangle } from "lucide-react";
import { Modal } from "@/shared/components/ui/Modal";
import { Badge } from "@/shared/components/ui/Badge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";
import { AnalysisBody, ConfidenceBar, EngineBadge } from "./AnalysisParts";

interface AnalysisDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  analysis: AnalysisResultDto;
  sourceName?: string;
}

export function AnalysisDetailModal({
  isOpen,
  onClose,
  analysis,
  sourceName,
}: AnalysisDetailModalProps) {
  const t = useTranslations("analyses");
  const dates = useDateFormat();

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t("detailTitle", { id: analysis.id })}
      description={`${t("analyzedAt")}: ${dates.dateTime(analysis.analyzedAt)}`}
      size="lg"
    >
      <div className="row">
        <EngineBadge engine={analysis.engineType} />
        {sourceName && <Badge variant="outline">{sourceName}</Badge>}
      </div>

      <ConfidenceBar score={analysis.confidenceScore} engine={analysis.engineType} />

      <div className="well p-4">
        <AnalysisBody analysis={analysis} />
      </div>

      {analysis.affectedDeployment && (
        <div className="banner c-warn" role="note">
          <AlertTriangle size={16} aria-hidden="true" />
          <div>
            <p className="font-medium">{t("affectedDeploymentTitle")}</p>
            <p className="small t2">
              {t("affectedDeploymentText", { deployment: analysis.affectedDeployment })}
            </p>
          </div>
        </div>
      )}
    </Modal>
  );
}
