"use client";

import { useState } from "react";
import Swal from "sweetalert2";

import { motion } from "framer-motion";
import { MoreVertical, Wifi, WifiOff, TestTube } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Dropdown, type DropdownItem } from "@/shared/components/ui/Dropdown";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import { ConnectionStatus, SOURCE_TYPE_LABELS } from "@/shared/types/common.types";
import { formatRelativeTime } from "@/shared/lib/utils";
import {
  useDeleteSourceMutation,
  useTestConnectionMutation,
  useToggleSourceMutation,
} from "@/features/sources/api/useSourceMutation";
import { useCan } from "@/features/auth/hooks/useCan";
import { cn } from "@/shared/lib/utils";
import { SourceDetailModal } from "./SourceDetailModal";

/* ── Status Dot ────────────────────────────────────────────── */

function StatusDot({ status }: { status: ConnectionStatus }) {
  const colors: Record<ConnectionStatus, string> = {
    [ConnectionStatus.CONNECTED]: "bg-success",
    [ConnectionStatus.WARNING]: "bg-warning",
    [ConnectionStatus.DISCONNECTED]: "bg-error",
  };

  const labels: Record<ConnectionStatus, string> = {
    [ConnectionStatus.CONNECTED]: "Bağlı",
    [ConnectionStatus.WARNING]: "Uyarı",
    [ConnectionStatus.DISCONNECTED]: "Bağlantı Yok",
  };

  return (
    <div className="flex items-center gap-1.5">
      <span className="relative flex h-2.5 w-2.5">
        {status === ConnectionStatus.CONNECTED && (
          <span
            className={cn(
              "animate-ping absolute inline-flex h-full w-full rounded-full opacity-75",
              colors[status],
            )}
          />
        )}
        <span className={cn("relative inline-flex rounded-full h-2.5 w-2.5", colors[status])} />
      </span>
      <span className="text-xs text-text-secondary">{labels[status]}</span>
    </div>
  );
}

/* ── Props ─────────────────────────────────────────────────── */

interface SourceCardProps {
  source: LogSourceDto;
  index: number;
}

/* ── Component ─────────────────────────────────────────────── */

function SourceCard({ source, index }: SourceCardProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const testConnection = useTestConnectionMutation();
  const toggleSource = useToggleSourceMutation();
  const deleteSource = useDeleteSourceMutation();
  const canEdit = useCan("edit");
  const canDelete = useCan("delete");
  const config = source.config ? JSON.parse(source.config) : {};

  // The API enforces the same rules (EDITOR: toggle/test, ADMIN: delete); hiding avoids 403s.
  const menuItems: DropdownItem[] = [];
  if (canEdit) {
    menuItems.push({
      label: source.enabled ? "Devre Dışı Bırak" : "Etkinleştir",
      value: "toggle",
      icon: source.enabled ? <WifiOff size={14} /> : <Wifi size={14} />,
    });
  }
  if (canDelete) menuItems.push({ label: "Sil", value: "delete", danger: true });

  const handleMenu = async (value: string) => {
    if (value === "toggle") {
      toggleSource.mutate({ id: source.id, enabled: !source.enabled });
      return;
    }
    if (value === "delete") {
      const result = await Swal.fire({
        title: "Kaynak silinsin mi?",
        text: `"${source.name}" silinir; toplanmış loglar saklama süresi dolana kadar kalır.`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#ef4444",
        cancelButtonColor: "#3b82f6",
        confirmButtonText: "Evet, Sil",
        cancelButtonText: "İptal",
      });
      if (result.isConfirmed) deleteSource.mutate(source.id);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1, duration: 0.3 }}
    >
      <Card variant="default" hover glow onClick={() => setIsModalOpen(true)}>
        {/* Header */}
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-2">
            <StatusDot status={source.status ?? ConnectionStatus.DISCONNECTED} />
          </div>
          {menuItems.length > 0 && (
            // Menu clicks must not bubble to the card (which opens the detail modal).
            <div onClick={(e) => e.stopPropagation()}>
              <Dropdown
                trigger={
                  <button
                    className="p-1 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary transition-colors"
                    aria-label="Kaynak işlemleri"
                  >
                    <MoreVertical size={16} />
                  </button>
                }
                items={menuItems}
                onSelect={(value) => void handleMenu(value)}
              />
            </div>
          )}
        </div>

        {/* Name & Type */}
        <h3 className="text-base font-semibold font-display text-text-primary mb-1">
          {source.name}
        </h3>
        <Badge variant="outline" size="sm">
          {SOURCE_TYPE_LABELS[source.type]}
        </Badge>

        {/* Details */}
        <div className="mt-3 space-y-1.5">
          {config.namespace && (
            <p className="text-xs text-text-muted">
              Namespace: <span className="text-text-secondary font-mono">{config.namespace}</span>
            </p>
          )}
          {config.host && (
            <p className="text-xs text-text-muted">
              Host: <span className="text-text-secondary font-mono">{config.host}</span>
            </p>
          )}
          <p className="text-xs text-text-muted">
            Son log: {formatRelativeTime(source.lastSeenAt)}
          </p>
        </div>

        {/* Actions */}
        {canEdit && (
          <div className="flex gap-2 mt-4">
            <Button
              variant="secondary"
              size="sm"
              fullWidth
              icon={<TestTube size={14} />}
              loading={testConnection.isPending}
              onClick={(e) => {
                e.stopPropagation();
                testConnection.mutate(source.id);
              }}
            >
              Test
            </Button>
          </div>
        )}
      </Card>

      <SourceDetailModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        source={source}
      />
    </motion.div>
  );
}

export { SourceCard };
