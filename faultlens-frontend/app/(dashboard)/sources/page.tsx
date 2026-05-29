"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Button } from "@/shared/components/ui/Button";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { SourceCard } from "@/features/sources/components/SourceCard";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { AddSourceModal } from "@/features/sources/components/AddSourceModal";

export default function SourcesPage() {
  const { data: sources, isLoading, error } = useSourcesQuery();
  const [showModal, setShowModal] = useState(false);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Kaynaklar"
        description="Log kaynaklarını yönetin ve izleyin"
        action={
          <Button
            variant="primary"
            icon={<Plus size={16} />}
            onClick={() => setShowModal(true)}
          >
            Yeni Kaynak Ekle
          </Button>
        }
      />

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : sources && sources.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {sources.map((source, index) => (
            <SourceCard key={source.id} source={source} index={index} />
          ))}
        </div>
      ) : (
        <EmptyState
          title="Henüz kaynak yok"
          description="Log kaynaklarınızı ekleyerek izlemeye başlayın"
          action={{
            label: "İlk Kaynağı Ekle",
            onClick: () => setShowModal(true),
          }}
        />
      )}

      <AddSourceModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
      />
    </div>
  );
}
