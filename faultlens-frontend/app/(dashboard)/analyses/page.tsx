"use client";

import { PageHeader } from "@/shared/components/layout/PageHeader";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { AnalysisCard } from "@/features/analyses/components/AnalysisCard";
import { useAnalyses } from "@/features/analyses/api/analyses.queries";
import { Brain } from "lucide-react";

export default function AnalysesPage() {
  const { data: analyses, isLoading } = useAnalyses();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analizler"
        description="Hata gruplarının kök neden analizleri ve öneriler"
      />

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : analyses && analyses.length > 0 ? (
        <div className="space-y-4">
          {analyses.map((analysis, index) => (
            <AnalysisCard
              key={analysis.id}
              analysis={analysis}
              index={index}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={<Brain size={28} />}
          title="Henüz analiz yok"
          description="Hata grupları analiz edildiğinde burada görünecek"
        />
      )}
    </div>
  );
}
