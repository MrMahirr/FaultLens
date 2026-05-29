"use client";

import { use } from "react";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { Button } from "@/shared/components/ui/Button";
import { SeverityBadge } from "@/features/logs/components/SeverityBadge";
import { Card } from "@/shared/components/ui/Card";
import { useLogDetailQuery } from "@/features/logs/api/useLogQuery";
import { formatDate } from "@/shared/lib/utils";
import { LoadingSpinner } from "@/shared/components/feedback/LoadingSpinner";
import { cn } from "@/shared/lib/utils";

export default function LogDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const logId = parseInt(id, 10);
  const { data: detailData, isLoading, error } = useLogDetailQuery(id);
  const log = detailData?.log;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <LoadingSpinner label="Log yükleniyor..." />
      </div>
    );
  }

  if (error || !log) {
    return (
      <div className="text-center py-16">
        <p className="text-text-secondary mb-4">Log bulunamadı.</p>
        <Link href="/logs">
          <Button variant="secondary" icon={<ArrowLeft size={16} />}>
            Geri Dön
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Back Button */}
      <Link href="/logs">
        <Button variant="ghost" size="sm" icon={<ArrowLeft size={14} />}>
          Logs&apos;a Dön
        </Button>
      </Link>

      {/* Header */}
      <div className="flex items-center gap-3">
        <SeverityBadge severity={log.severity} />
        <span className="text-sm font-mono text-text-muted">Log #{log.id}</span>
        <span className="text-sm text-text-muted">
          {formatDate(log.timestamp)}
        </span>
      </div>

      {/* Message */}
      <Card variant="default">
        <h2 className="text-xs font-medium text-text-muted uppercase tracking-wider mb-2">
          Mesaj
        </h2>
        <p className="text-text-primary leading-relaxed">{log.message}</p>
      </Card>

      {/* Details Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: "Servis", value: log.serviceName ?? "—" },
          { label: "Kaynak ID", value: log.sourceId },
          { label: "Namespace", value: log.namespace },
          { label: "Pod", value: log.podName ?? "—" },
        ].map((item) => (
          <Card key={item.label} variant="bordered" padding="sm">
            <p className="text-[10px] text-text-muted uppercase tracking-wider">
              {item.label}
            </p>
            <p className="text-sm font-mono text-text-primary mt-1">
              {item.value}
            </p>
          </Card>
        ))}
      </div>

      {/* Stack Trace */}
      {log.stackTrace && (
        <div>
          <h2 className="text-xs font-medium text-text-muted uppercase tracking-wider mb-2">
            Stack Trace
          </h2>
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

      {/* Analyses */}
      {detailData?.analyses && detailData.analyses.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-xs font-medium text-text-muted uppercase tracking-wider">
            AI & Kural Tabanlı Kök Neden Analizi
          </h2>
          <div className="grid grid-cols-1 gap-4">
            {detailData.analyses.map((analysis: any, i: number) => (
              <Card key={analysis.id || i} variant="default" className="border-l-4 border-l-accent">
                <p className="text-sm font-semibold text-text-primary uppercase tracking-wider">
                  {analysis.engineType === "AI_ANALYSIS" ? "AI Analizi" : "Kural Tabanlı Analiz"}
                </p>
                <p className="text-sm text-text-secondary mt-2">
                  <strong>Kök Neden:</strong> {analysis.rootCause}
                </p>
                <p className="text-sm text-text-secondary mt-1">
                  <strong>Öneri:</strong> {analysis.suggestion}
                </p>
                {analysis.confidenceScore && (
                  <p className="text-[10px] text-accent mt-2 font-mono">
                    Güven Oranı: {Math.round(analysis.confidenceScore * 100)}%
                  </p>
                )}
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
