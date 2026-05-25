"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { Plus, Rocket, AlertCircle } from "lucide-react";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Modal } from "@/shared/components/ui/Modal";
import { Input } from "@/shared/components/ui/Input";
import { DropdownSelect, type DropdownItem } from "@/shared/components/ui/Dropdown";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { useDeployments, useCreateDeployment } from "@/features/deployments/api/deployments.queries";
import {
  DeploymentStatus,
  DeploymentEnvironment,
} from "@/shared/types/common.types";
import { formatDate } from "@/shared/lib/utils";
import { cn } from "@/shared/lib/utils";

/* ── Status Badge Variant Map ──────────────────────────────── */

function getStatusVariant(status: DeploymentStatus) {
  switch (status) {
    case DeploymentStatus.SUCCESS:
      return "success" as const;
    case DeploymentStatus.FAILED:
      return "error" as const;
    case DeploymentStatus.ROLLED_BACK:
      return "warning" as const;
    case DeploymentStatus.IN_PROGRESS:
      return "info" as const;
  }
}

function getStatusLabel(status: DeploymentStatus) {
  switch (status) {
    case DeploymentStatus.SUCCESS:
      return "Başarılı";
    case DeploymentStatus.FAILED:
      return "Başarısız";
    case DeploymentStatus.ROLLED_BACK:
      return "Geri Alındı";
    case DeploymentStatus.IN_PROGRESS:
      return "Devam Ediyor";
  }
}

function getEnvLabel(env: DeploymentEnvironment) {
  switch (env) {
    case DeploymentEnvironment.PRODUCTION:
      return "Production";
    case DeploymentEnvironment.STAGING:
      return "Staging";
    case DeploymentEnvironment.DEVELOPMENT:
      return "Development";
  }
}

/* ── Component ─────────────────────────────────────────────── */

export default function DeploymentsPage() {
  const { data: deployments, isLoading } = useDeployments();
  const createDeployment = useCreateDeployment();
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    serviceName: "",
    version: "",
    environment: "",
  });

  const envOptions: DropdownItem[] = [
    { label: "Production", value: DeploymentEnvironment.PRODUCTION },
    { label: "Staging", value: DeploymentEnvironment.STAGING },
    { label: "Development", value: DeploymentEnvironment.DEVELOPMENT },
  ];

  const handleCreate = async () => {
    await createDeployment.mutateAsync({
      serviceName: formData.serviceName,
      version: formData.version,
      environment: formData.environment as DeploymentEnvironment,
      status: DeploymentStatus.SUCCESS,
      deployedBy: "admin",
      deployedAt: new Date().toISOString(),
      errorCount: 0,
    });
    setShowModal(false);
    setFormData({ serviceName: "", version: "", environment: "" });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Deployments"
        description="Deployment geçmişi ve hata korelasyonları"
        action={
          <Button
            variant="primary"
            icon={<Plus size={16} />}
            onClick={() => setShowModal(true)}
          >
            Yeni Deployment
          </Button>
        }
      />

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : deployments && deployments.length > 0 ? (
        /* Timeline */
        <div className="relative">
          {/* Timeline Line */}
          <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-gradient-to-b from-accent via-border-default to-transparent" />

          <div className="space-y-4">
            {deployments.map((deployment, index) => (
              <motion.div
                key={deployment.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.1, duration: 0.3 }}
                className="relative pl-14"
              >
                {/* Timeline Dot */}
                <div
                  className={cn(
                    "absolute left-[18px] top-6 w-3 h-3 rounded-full border-2 border-bg-primary z-10",
                    deployment.status === DeploymentStatus.SUCCESS
                      ? "bg-success"
                      : deployment.status === DeploymentStatus.FAILED
                        ? "bg-error"
                        : "bg-warning"
                  )}
                />

                <Card variant="default" hover>
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold font-display text-text-primary">
                          {deployment.serviceName}
                        </h3>
                        <Badge variant="accent" size="sm">
                          {deployment.version}
                        </Badge>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-text-muted">
                        <span>{formatDate(deployment.deployedAt)}</span>
                        <span>by {deployment.deployedBy}</span>
                      </div>
                      {deployment.description && (
                        <p className="text-sm text-text-secondary mt-2">
                          {deployment.description}
                        </p>
                      )}
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <Badge
                        variant={getStatusVariant(deployment.status)}
                        size="sm"
                      >
                        {getStatusLabel(deployment.status)}
                      </Badge>
                      <Badge variant="outline" size="sm">
                        {getEnvLabel(deployment.environment)}
                      </Badge>
                      {deployment.errorCount > 0 && (
                        <Badge variant="error" size="sm">
                          <AlertCircle size={10} />
                          {deployment.errorCount}
                        </Badge>
                      )}
                    </div>
                  </div>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      ) : (
        <EmptyState
          icon={<Rocket size={28} />}
          title="Henüz deployment yok"
          description="İlk deployment kaydını oluşturun"
          action={{
            label: "Deployment Ekle",
            onClick: () => setShowModal(true),
          }}
        />
      )}

      {/* Add Deployment Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title="Yeni Deployment Kaydet"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              İptal
            </Button>
            <Button
              variant="primary"
              onClick={handleCreate}
              loading={createDeployment.isPending}
              disabled={!formData.serviceName || !formData.version}
            >
              Kaydet
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input
            label="Servis Adı"
            placeholder="payment-service"
            value={formData.serviceName}
            onChange={(e) =>
              setFormData((prev) => ({ ...prev, serviceName: e.target.value }))
            }
          />
          <Input
            label="Versiyon"
            placeholder="v2.3.1"
            value={formData.version}
            onChange={(e) =>
              setFormData((prev) => ({ ...prev, version: e.target.value }))
            }
          />
          <DropdownSelect
            label="Ortam"
            value={formData.environment}
            options={envOptions}
            onChange={(value) =>
              setFormData((prev) => ({ ...prev, environment: value }))
            }
            placeholder="Ortam seçin"
          />
        </div>
      </Modal>
    </div>
  );
}
