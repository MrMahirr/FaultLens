"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Plus, Server } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Button } from "@/shared/components/ui/Button";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { ErrorState } from "@/shared/components/feedback/ErrorState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { SourceCard } from "@/features/sources/components/SourceCard";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { AddSourceModal } from "@/features/sources/components/AddSourceModal";

export default function SourcesPage() {
  const t = useTranslations("sources");
  const { data: sources, isLoading, error, refetch } = useSourcesQuery();
  const [showModal, setShowModal] = useState(false);
  const canEdit = useCan("edit");

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        action={
          canEdit && (
            <Button
              variant="primary"
              icon={<Plus size={16} aria-hidden="true" />}
              onClick={() => setShowModal(true)}
            >
              {t("add")}
            </Button>
          )
        }
      />

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4" aria-busy="true">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : error ? (
        <ErrorState error={error as Error} onRetry={() => void refetch()} />
      ) : sources && sources.length > 0 ? (
        <ul
          className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 m-0 p-0"
          aria-label={t("list")}
        >
          {sources.map((source, index) => (
            <SourceCard key={source.id} source={source} index={index} />
          ))}
        </ul>
      ) : (
        <div className="card">
          <EmptyState
            icon={<Server size={18} />}
            title={t("emptyTitle")}
            description={t("emptyDescription")}
            action={
              canEdit ? { label: t("addFirst"), onClick: () => setShowModal(true) } : undefined
            }
          />
        </div>
      )}

      <AddSourceModal isOpen={showModal} onClose={() => setShowModal(false)} />
    </>
  );
}
