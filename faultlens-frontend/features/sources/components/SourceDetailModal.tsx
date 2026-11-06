"use client";

import { useTranslations } from "next-intl";
import { Modal } from "@/shared/components/ui/Modal";
import { Badge } from "@/shared/components/ui/Badge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import { SourceStatus } from "./SourceStatus";

interface SourceDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  source: LogSourceDto | null;
}

/** Secrets are never shown again once saved (Instrument security patterns). */
const SECRET_KEYS = /pass|secret|token|key/i;

export function SourceDetailModal({ isOpen, onClose, source }: SourceDetailModalProps) {
  const t = useTranslations("sources");
  const dates = useDateFormat();
  if (!source) return null;

  let config: Record<string, unknown> = {};
  try {
    config = source.config ? JSON.parse(source.config) : {};
  } catch {
    config = {};
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={source.name} description={t("detail.title")}>
      <div className="row">
        <Badge variant="outline">{t(`types.${source.type}`)}</Badge>
        <Badge variant={source.enabled ? "success" : "default"} dot>
          {source.enabled ? t("enabled") : t("disabledBadge")}
        </Badge>
        <SourceStatus status={source.status} />
      </div>

      <dl className="kv">
        <dt>{t("detail.createdAt")}</dt>
        <dd className="mono">{dates.dateTime(source.createdAt)}</dd>
        <dt>{t("detail.lastSeen")}</dt>
        <dd>{dates.relative(source.lastSeenAt)}</dd>
        {source.health && (
          <>
            <dt>{t("detail.health")}</dt>
            <dd className="mono">{source.health}</dd>
          </>
        )}
        {source.collectorInstance && (
          <>
            <dt>{t("detail.collector")}</dt>
            <dd className="mono break-all">{source.collectorInstance}</dd>
          </>
        )}
      </dl>

      {Object.keys(config).length > 0 && (
        <section className="field">
          <h3 className="eyebrow">{t("detail.config")}</h3>
          <dl className="kv well p-3">
            {Object.entries(config).map(([key, value]) => (
              <div key={key} className="contents">
                <dt className="mono">{key}</dt>
                <dd className="mono break-all">
                  {SECRET_KEYS.test(key) ? "••••••••" : String(value)}
                </dd>
              </div>
            ))}
          </dl>
        </section>
      )}
    </Modal>
  );
}
