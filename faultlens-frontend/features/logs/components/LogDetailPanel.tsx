"use client";

import { motion } from "framer-motion";
import { X, Clock, Server, Layers, Code, Lightbulb, Brain } from "lucide-react";
import { SeverityBadge } from "@/features/logs/components/SeverityBadge";
import { Card } from "@/shared/components/ui/Card";
import { Button } from "@/shared/components/ui/Button";
import type { LogEntryDto } from "@/features/logs/types/log.types";
import { formatDate } from "@/shared/lib/utils";
import { cn } from "@/shared/lib/utils";
import { useLogDetail } from "@/features/logs/api/logs.queries";

/* ── Props ─────────────────────────────────────────────────── */

interface LogDetailPanelProps {
  log: LogEntryDto | null;
  onClose: () => void;
}

/* ── Component ─────────────────────────────────────────────── */

function LogDetailPanel({ log, onClose }: LogDetailPanelProps) {
  const { data: detailData, isLoading: isDetailLoading } = useLogDetail(log?.id ?? 0);
  const analyses = detailData?.analyses || [];

  if (!log) return null;

  return (
    <motion.div
      initial={{ x: "100%", opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      exit={{ x: "100%", opacity: 0 }}
      transition={{ type: "spring", damping: 30, stiffness: 300 }}
      className={cn(
        "fixed right-0 top-0 h-screen w-full sm:w-[480px] z-50",
        "bg-bg-secondary border-l border-border-default",
        "shadow-2xl overflow-y-auto"
      )}
    >
      {/* Header */}
      <div className="sticky top-0 bg-bg-secondary border-b border-border-default px-6 py-4 flex items-center justify-between z-10">
        <div className="flex items-center gap-3">
          <SeverityBadge severity={log.severity} />
          <span className="text-sm font-mono text-text-muted">#{log.id}</span>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary transition-colors"
          aria-label="Paneli kapat"
        >
          <X size={18} />
        </button>
      </div>

      {/* Content */}
      <div className="p-6 space-y-6">
        {/* Meta Info */}
        <div className="grid grid-cols-2 gap-4">
          <div className="flex items-center gap-2">
            <Clock size={14} className="text-text-muted shrink-0" />
            <div>
              <p className="text-[10px] text-text-muted uppercase tracking-wider">
                Zaman
              </p>
              <p className="text-xs font-mono text-text-primary">
                {formatDate(log.timestamp)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Server size={14} className="text-text-muted shrink-0" />
            <div>
              <p className="text-[10px] text-text-muted uppercase tracking-wider">
                Kaynak
              </p>
              <p className="text-xs font-mono text-text-primary">
                {log.serviceName ?? `Source #${log.sourceId}`}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Layers size={14} className="text-text-muted shrink-0" />
            <div>
              <p className="text-[10px] text-text-muted uppercase tracking-wider">
                Namespace
              </p>
              <p className="text-xs font-mono text-text-primary">
                {log.namespace}
              </p>
            </div>
          </div>

          {log.podName && (
            <div className="flex items-center gap-2">
              <Code size={14} className="text-text-muted shrink-0" />
              <div>
                <p className="text-[10px] text-text-muted uppercase tracking-wider">
                  Pod
                </p>
                <p className="text-xs font-mono text-text-primary">
                  {log.podName}
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Message */}
        <div>
          <h3 className="text-xs font-medium text-text-muted uppercase tracking-wider mb-2">
            Mesaj
          </h3>
          <Card variant="bordered" padding="sm">
            <p className="text-sm text-text-primary leading-relaxed">
              {log.message}
            </p>
          </Card>
        </div>

        {/* Stack Trace */}
        {log.stackTrace && (
          <div>
            <h3 className="text-xs font-medium text-text-muted uppercase tracking-wider mb-2">
              Stack Trace
            </h3>
            <div className="bg-bg-primary border border-border-default rounded-xl p-4 overflow-x-auto">
              <pre className="font-mono text-xs leading-relaxed">
                {log.stackTrace.split("\n").map((line, i) => (
                  <div
                    key={i}
                    className={cn(
                      "py-0.5",
                      line.includes("Error") || line.includes("Exception")
                        ? "text-error font-medium"
                        : line.trim().startsWith("at")
                          ? "text-text-muted pl-4"
                          : "text-text-secondary"
                    )}
                  >
                    {line}
                  </div>
                ))}
              </pre>
            </div>
          </div>
        )}

        {/* Analysis Suggestion */}
        <div>
          <h3 className="text-xs font-medium text-text-muted uppercase tracking-wider mb-2">
            Kök Neden & Analiz
          </h3>
          {isDetailLoading ? (
            <div className="flex items-center gap-2 text-xs text-text-muted">
              <div className="w-3.5 h-3.5 border border-accent border-t-transparent rounded-full animate-spin" />
              <span>Analizler yükleniyor...</span>
            </div>
          ) : analyses && analyses.length > 0 ? (
            <div className="space-y-3">
              {analyses.map((analysis: any, i: number) => (
                <Card key={analysis.id || i} variant="glass" padding="sm" className="border-l-2 border-l-accent">
                  <div className="flex items-start gap-2">
                    <Brain size={14} className="text-accent shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <p className="text-xs font-semibold text-text-primary uppercase tracking-wider">
                        {analysis.engineType === "AI_ANALYSIS" ? "AI Analizi" : "Kural Tabanlı Analiz"}
                      </p>
                      <p className="text-xs text-text-secondary mt-1">
                        <strong>Kök Neden:</strong> {analysis.rootCause}
                      </p>
                      <p className="text-xs text-text-secondary mt-1">
                        <strong>Öneri:</strong> {analysis.suggestion}
                      </p>
                      {analysis.confidenceScore && (
                        <p className="text-[10px] text-accent mt-2 font-mono">
                          Güven: {Math.round(analysis.confidenceScore * 100)}%
                        </p>
                      )}
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          ) : log.groupId ? (
            <Card variant="glass" padding="sm">
              <div className="flex items-start gap-2">
                <Lightbulb
                  size={14}
                  className="text-warning shrink-0 mt-0.5"
                />
                <div>
                  <p className="text-sm text-text-primary">
                    Bu log bir hata grubuna ait. Analiz sayfasından detaylı kök
                    neden analizi görüntüleyebilirsiniz.
                  </p>
                  <Button variant="ghost" size="sm" className="mt-2">
                    Analize Git →
                  </Button>
                </div>
              </div>
            </Card>
          ) : (
            <p className="text-xs text-text-muted">
              Bu log için henüz analiz bulunmuyor.
            </p>
          )}
        </div>
      </div>
    </motion.div>
  );
}

export { LogDetailPanel };
