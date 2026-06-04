import { Modal } from "@/shared/components/ui/Modal";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import { Badge } from "@/shared/components/ui/Badge";
import { SOURCE_TYPE_LABELS, ConnectionStatus } from "@/shared/types/common.types";
import { formatRelativeTime } from "@/shared/lib/utils";
import { CheckCircle2, AlertTriangle, XCircle } from "lucide-react";

interface SourceDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  source: LogSourceDto | null;
}

export function SourceDetailModal({ isOpen, onClose, source }: SourceDetailModalProps) {
  if (!source) return null;

  const config = source.config ? JSON.parse(source.config) : {};

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Kaynak Detayları"
      size="md"
    >
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-xl font-display font-semibold text-text-primary">
              {source.name}
            </h3>
            <div className="flex items-center gap-2 mt-2">
              <Badge variant="outline">{SOURCE_TYPE_LABELS[source.type]}</Badge>
              {source.enabled ? (
                <Badge variant="success">Etkin</Badge>
              ) : (
                <Badge variant="error">Devre Dışı</Badge>
              )}
            </div>
          </div>
          <div className="text-right">
            <span className="text-sm text-text-muted">Bağlantı Durumu</span>
            <div className="flex items-center justify-end gap-1.5 mt-1">
              {source.status === ConnectionStatus.CONNECTED && (
                <><CheckCircle2 size={16} className="text-success" /> <span className="text-success text-sm">Bağlı</span></>
              )}
              {source.status === ConnectionStatus.WARNING && (
                <><AlertTriangle size={16} className="text-warning" /> <span className="text-warning text-sm">Uyarı</span></>
              )}
              {source.status === ConnectionStatus.DISCONNECTED && (
                <><XCircle size={16} className="text-error" /> <span className="text-error text-sm">Bağlantı Yok</span></>
              )}
            </div>
          </div>
        </div>

        <div className="bg-bg-tertiary rounded-lg p-4 space-y-3">
          <h4 className="text-sm font-medium text-text-primary">Zaman Bilgileri</h4>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-text-muted mb-1">Oluşturulma Tarihi</p>
              <p className="text-sm text-text-secondary">
                {source.createdAt ? new Date(source.createdAt).toLocaleString("tr-TR") : "-"}
              </p>
            </div>
            <div>
              <p className="text-xs text-text-muted mb-1">Son Görülme</p>
              <p className="text-sm text-text-secondary">
                {source.lastSeenAt ? formatRelativeTime(source.lastSeenAt) : "-"}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-bg-tertiary rounded-lg p-4 space-y-3">
          <h4 className="text-sm font-medium text-text-primary">Konfigürasyon Detayları</h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {Object.entries(config).map(([key, value]) => (
              <div key={key} className="break-all">
                <p className="text-xs text-text-muted mb-1 capitalize">{key}</p>
                <p className="text-sm text-text-secondary font-mono" title={String(value)}>
                  {String(value)}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Modal>
  );
}
