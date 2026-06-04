"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { Plus, Rocket, AlertCircle, Search, Terminal, ExternalLink } from "lucide-react";
import Link from "next/link";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Card } from "@/shared/components/ui/Card";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Modal } from "@/shared/components/ui/Modal";
import { Input } from "@/shared/components/ui/Input";
import { DropdownSelect, type DropdownItem } from "@/shared/components/ui/Dropdown";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { useDeploymentsQuery } from "@/features/deployments/api/useDeploymentQuery";
import { useCreateDeploymentMutation } from "@/features/deployments/api/useDeploymentMutation";
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
  const { data: deployments, isLoading, error } = useDeploymentsQuery();
  const createDeployment = useCreateDeploymentMutation();
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    serviceName: "",
    version: "",
    environment: "",
  });

  const [searchQuery, setSearchQuery] = useState("");
  const [envFilter, setEnvFilter] = useState<DeploymentEnvironment | "ALL">("ALL");

  const envOptions: DropdownItem[] = [
    { label: "Production", value: DeploymentEnvironment.PRODUCTION },
    { label: "Staging", value: DeploymentEnvironment.STAGING },
    { label: "Development", value: DeploymentEnvironment.DEVELOPMENT },
  ];

  const filteredDeployments = deployments?.filter((d) => {
    const matchesSearch = d.serviceName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesEnv = envFilter === "ALL" || d.environment === envFilter;
    return matchesSearch && matchesEnv;
  });

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

      {/* Webhook Integration Guide */}
      <Card variant="bordered" className="bg-accent/5 border-accent/20">
        <div className="flex flex-col sm:flex-row items-start gap-4">
          <div className="p-3 bg-accent/10 rounded-xl text-accent shrink-0">
            <Terminal size={22} />
          </div>
          <div className="flex-1 w-full min-w-0">
            <h3 className="font-semibold text-text-primary mb-1">CI/CD Entegrasyonu</h3>
            <p className="text-sm text-text-secondary mb-4 leading-relaxed">
              Deployment'larınızı otomatik takip etmek için CI/CD pipeline'ınıza (GitHub Actions, vb.) aşağıdaki webhook isteğini ekleyin.
            </p>
            <div className="bg-bg-primary p-4 rounded-xl border border-border-default overflow-x-auto w-full shadow-inner">
              <code className="text-[13px] text-text-primary font-mono whitespace-nowrap block">
                curl -X POST https://api.faultlens.com/api/v1/deployments -H "Content-Type: application/json" -d '&#123;"serviceName":"my-service","version":"v1.0.0","environment":"PRODUCTION"&#125;'
              </code>
            </div>
          </div>
        </div>
      </Card>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between bg-bg-secondary p-2 rounded-2xl border border-border-default">
        <div className="w-full sm:max-w-md">
          <Input 
            placeholder="Servis adı ile ara..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            icon={<Search size={18} />}
            className="border-transparent bg-transparent hover:bg-bg-tertiary focus:bg-bg-tertiary transition-colors shadow-none"
          />
        </div>
        <div className="w-full sm:w-56 shrink-0 pl-0 sm:pl-3 sm:border-l border-border-default">
          <DropdownSelect
            value={envFilter}
            options={[{label: "Tüm Ortamlar", value: "ALL"}, ...envOptions]}
            onChange={(v) => setEnvFilter(v as any)}
            placeholder="Ortam Filtresi"
          />
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : filteredDeployments && filteredDeployments.length > 0 ? (
        /* Timeline */
        <div className="relative">
          {/* Timeline Line */}
          <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-gradient-to-b from-accent via-border-default to-transparent" />

          <div className="space-y-4">
            {filteredDeployments.map((deployment, index) => (
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

                <Card 
                  variant="default" 
                  hover
                  className={cn(
                    "transition-all duration-300",
                    deployment.errorCount > 10 && "shadow-[0_0_15px_rgba(239,68,68,0.15)] border-error/50"
                  )}
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
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

                    <div className="flex flex-col items-start sm:items-end gap-3 shrink-0 mt-5 sm:mt-0 w-full sm:w-auto">
                      <div className="flex flex-wrap items-center gap-2">
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
                          <Badge variant="error" size="sm" className={cn(deployment.errorCount > 10 && "animate-pulse")}>
                            <AlertCircle size={10} />
                            {deployment.errorCount} Hata
                          </Badge>
                        )}
                      </div>
                      
                      <Link 
                        href={`/logs?service=${deployment.serviceName}&from=${new Date(deployment.deployedAt).getTime()}`}
                        className="text-[13px] font-medium flex items-center gap-1.5 text-accent hover:text-accent-hover transition-colors px-3 py-1.5 bg-accent/10 hover:bg-accent/20 rounded-lg"
                      >
                        <ExternalLink size={14} />
                        Logları İncele
                      </Link>
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
