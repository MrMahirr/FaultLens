"use client";

import { useEffect, useId, useRef } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { useTranslations } from "next-intl";
import { X, ExternalLink } from "lucide-react";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { useFocusTrap } from "@/shared/hooks/useFocusTrap";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import type { LogEntryDto } from "@/features/logs/types/log.types";
import { useLogDetailQuery } from "@/features/logs/api/useLogQuery";
import { StackTrace } from "./StackTrace";
import { LogAnalyses } from "./LogAnalyses";

/* ── Props ─────────────────────────────────────────────────── */

interface LogDetailPanelProps {
  log: LogEntryDto | null;
  onClose: () => void;
}

/* ── Component ─────────────────────────────────────────────── */

/** Side drawer with one log: a modal dialog (focus trapped, Esc / backdrop close). */
function LogDetailPanel({ log, onClose }: LogDetailPanelProps) {
  const t = useTranslations("logs.detail");
  const dates = useDateFormat();
  const panelRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const { data: detailData, isLoading } = useLogDetailQuery(log?.id ?? 0);

  useFocusTrap(panelRef, !!log);

  useEffect(() => {
    if (!log) return;
    const onKeyDown = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [log, onClose]);

  if (!log) return null;

  const meta: [string, string | undefined][] = [
    [t("time"), dates.dateTimeLong(log.timestamp)],
    [t("source"), log.serviceName ?? `#${log.sourceId}`],
    [t("namespace"), log.namespace],
    [t("pod"), log.podName],
    [t("container"), log.containerName],
  ];

  return (
    <motion.div
      className="fixed inset-0 z-50 flex justify-end"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.15 }}
    >
      <div
        className="absolute inset-0"
        style={{ background: "var(--scrim)" }}
        onClick={onClose}
        aria-hidden="true"
      />

      <motion.div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        initial={{ x: 24, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        exit={{ x: 24, opacity: 0 }}
        transition={{ duration: 0.2, ease: [0.2, 0, 0, 1] }}
        className="relative h-full w-full sm:w-[520px] bg-bg-raised border-l border-border-default overflow-y-auto outline-none"
        style={{ boxShadow: "var(--pop)" }}
      >
        <div className="panel-h sticky top-0 bg-bg-raised z-10">
          <div className="row">
            <h2 id={titleId} className="sr-only">
              {t("title")}
            </h2>
            <SeverityBadge severity={log.severity} />
            <span className="mono t3">#{log.id}</span>
          </div>
          <div className="row gap-1">
            <Link
              href={`/logs/${log.id}`}
              className="btn btn-ghost btn-sm btn-icon"
              aria-label={t("openPage")}
              title={t("openPage")}
            >
              <ExternalLink size={14} aria-hidden="true" />
            </Link>
            <button
              type="button"
              onClick={onClose}
              className="btn btn-ghost btn-sm btn-icon"
              aria-label={t("close")}
            >
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        </div>

        <div className="p-5 stack gap-5">
          <dl className="kv">
            {meta
              .filter(([, value]) => value)
              .map(([label, value]) => (
                <div key={label} className="contents">
                  <dt>{label}</dt>
                  <dd className="mono break-all">{value}</dd>
                </div>
              ))}
          </dl>

          <section className="field">
            <h3 className="eyebrow">{t("message")}</h3>
            <p className="well p-3 text-[14px] leading-relaxed break-words">{log.message}</p>
          </section>

          {log.stackTrace && (
            <section className="field">
              <h3 className="eyebrow">{t("stackTrace")}</h3>
              <StackTrace trace={log.stackTrace} />
            </section>
          )}

          <section className="field">
            <h3 className="eyebrow">{t("analysis")}</h3>
            <LogAnalyses log={log} analyses={detailData?.analyses ?? []} loading={isLoading} />
          </section>
        </div>
      </motion.div>
    </motion.div>
  );
}

export { LogDetailPanel };
