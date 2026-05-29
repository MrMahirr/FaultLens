"use client";

import { motion } from "framer-motion";
import { Card } from "@/shared/components/ui/Card";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { ConnectionStatus as ConnectionStatusEnum } from "@/shared/types/common.types";
import { SOURCE_TYPE_LABELS } from "@/shared/types/common.types";
import { formatRelativeTime } from "@/shared/lib/utils";
import { cn } from "@/shared/lib/utils";

/* ── Status Dot ────────────────────────────────────────────── */

function StatusDot({ status }: { status: ConnectionStatusEnum }) {
  const colorMap: Record<ConnectionStatusEnum, string> = {
    [ConnectionStatusEnum.CONNECTED]: "bg-success",
    [ConnectionStatusEnum.WARNING]: "bg-warning",
    [ConnectionStatusEnum.DISCONNECTED]: "bg-error",
  };

  return (
    <span className="relative flex h-2.5 w-2.5">
      {status === ConnectionStatusEnum.CONNECTED && (
        <span
          className={cn(
            "animate-ping absolute inline-flex h-full w-full rounded-full opacity-75",
            colorMap[status]
          )}
        />
      )}
      <span
        className={cn(
          "relative inline-flex rounded-full h-2.5 w-2.5",
          colorMap[status]
        )}
      />
    </span>
  );
}

/* ── Component ─────────────────────────────────────────────── */

function SystemHealthMap() {
  const { data: sources, isLoading } = useSourcesQuery();

  return (
    <Card variant="default" padding="none">
      <div className="px-5 pt-5 pb-3">
        <h3 className="text-sm font-semibold font-display text-text-primary">
          Sistem Sağlığı
        </h3>
        <p className="text-xs text-text-muted mt-0.5">
          {isLoading ? "Yükleniyor..." : "Kaynak durumları"}
        </p>
      </div>

      {!isLoading && sources && sources.length > 0 && (
        <div className="grid grid-cols-2 gap-3 px-5 pb-5">
        {(sources || []).slice(0, 4).map((source, index) => (
          <motion.div
            key={source.id}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: index * 0.1, duration: 0.3 }}
            className="bg-bg-tertiary/50 border border-border-default rounded-lg p-3 hover:border-border-hover transition-colors"
          >
            <div className="flex items-center gap-2 mb-2">
              <StatusDot status={source.status ?? ConnectionStatusEnum.CONNECTED} />
              <span className="text-xs font-medium text-text-primary truncate">
                {source.name}
              </span>
            </div>
            <div className="space-y-1">
              <p className="text-[10px] text-text-muted">
                {SOURCE_TYPE_LABELS[source.type]}
              </p>
              <p className="text-[10px] text-text-muted">
                Son log: {formatRelativeTime(source.lastSeenAt)}
              </p>
            </div>
          </motion.div>
        ))}
      </div>
      )}

      {!isLoading && (!sources || sources.length === 0) && (
        <div className="py-8 text-center text-sm text-text-muted">
          Bağlı kaynak bulunmuyor
        </div>
      )}
    </Card>
  );
}

export { SystemHealthMap };
