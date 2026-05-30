import { Modal } from "@/shared/components/ui/Modal";
import { Badge } from "@/shared/components/ui/Badge";
import { Alarm, AlarmStatus } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";
import { Clock, AlertTriangle, CheckCircle, Info } from "lucide-react";

interface AlarmDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  alarm: Alarm | null;
}

export function AlarmDetailModal({ isOpen, onClose, alarm }: AlarmDetailModalProps) {
  if (!alarm) return null;

  const isCritical = alarm.severity === Severity.CRITICAL || alarm.severity === Severity.ERROR;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Alarm Detayları"
      size="md"
    >
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col gap-3 pb-4 border-b border-border-default">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-text-primary flex items-center gap-2">
              {isCritical ? (
                <AlertTriangle className="w-5 h-5 text-error" />
              ) : (
                <Info className="w-5 h-5 text-warning" />
              )}
              {alarm.ruleName}
            </h3>
            <Badge
              variant={alarm.status === AlarmStatus.ACTIVE ? "error" : "success"}
              className={alarm.status === AlarmStatus.ACTIVE ? "animate-pulse" : ""}
            >
              {alarm.status === AlarmStatus.ACTIVE ? "Aktif" : "Çözüldü"}
            </Badge>
          </div>
          
          <div className="flex items-center gap-4 text-sm text-text-muted">
            <div className="flex items-center gap-1.5">
              <Clock className="w-4 h-4" />
              <span>
                Tetiklenme: {new Date(alarm.triggeredAt).toLocaleString("tr-TR")}
              </span>
            </div>
            {alarm.resolvedAt && (
              <div className="flex items-center gap-1.5 text-success">
                <CheckCircle className="w-4 h-4" />
                <span>
                  Çözülme: {new Date(alarm.resolvedAt).toLocaleString("tr-TR")}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Details */}
        <div className="space-y-4">
          <div>
            <h4 className="text-sm font-medium text-text-secondary mb-1">Seviye</h4>
            <Badge variant={isCritical ? "error" : "warning"}>
              {alarm.severity}
            </Badge>
          </div>
          
          <div>
            <h4 className="text-sm font-medium text-text-secondary mb-1">Mesaj</h4>
            <div className="p-3 bg-bg-secondary rounded-lg border border-border-default text-text-primary text-sm whitespace-pre-wrap break-words">
              {alarm.message}
            </div>
          </div>
          
          <div>
            <h4 className="text-sm font-medium text-text-secondary mb-1">Kural ID</h4>
            <div className="text-sm text-text-muted">
              {alarm.ruleId}
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}
