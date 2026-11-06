"use client";

import { useFormatter, useTranslations } from "next-intl";
import { Modal } from "@/shared/components/ui/Modal";
import { Badge } from "@/shared/components/ui/Badge";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { Alarm, AlarmStatus } from "../types/alarm.types";

interface AlarmDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  alarm: Alarm | null;
}

const STATUS_BADGE: Record<AlarmStatus, "error" | "warning" | "success"> = {
  [AlarmStatus.ACTIVE]: "error",
  [AlarmStatus.ACKNOWLEDGED]: "warning",
  [AlarmStatus.RESOLVED]: "success",
};

export function AlarmDetailModal({ isOpen, onClose, alarm }: AlarmDetailModalProps) {
  const t = useTranslations("alarms");
  const format = useFormatter();
  if (!alarm) return null;

  const when = (iso: string) =>
    format.dateTime(new Date(iso), { dateStyle: "medium", timeStyle: "medium" });

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={alarm.ruleName} description={t("detail.title")}>
      <dl className="kv">
        <dt>{t("detail.status")}</dt>
        <dd>
          <Badge variant={STATUS_BADGE[alarm.status]} dot>
            {t(`status.${alarm.status}`)}
          </Badge>
        </dd>
        <dt>{t("detail.severity")}</dt>
        <dd>
          <SeverityBadge severity={alarm.severity} />
        </dd>
        <dt>{t("detail.triggeredAt")}</dt>
        <dd className="mono">{when(alarm.triggeredAt)}</dd>
        {alarm.resolvedAt && (
          <>
            <dt>{t("detail.resolvedAt")}</dt>
            <dd className="mono text-success">{when(alarm.resolvedAt)}</dd>
          </>
        )}
        {alarm.ruleId && (
          <>
            <dt>{t("detail.ruleId")}</dt>
            <dd className="mono t3">{alarm.ruleId}</dd>
          </>
        )}
      </dl>

      <div className="field">
        <span className="label">{t("detail.message")}</span>
        <pre className="code whitespace-pre-wrap break-words">{alarm.message}</pre>
      </div>
    </Modal>
  );
}
