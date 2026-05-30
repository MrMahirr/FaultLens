"use client";

import { useState } from "react";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { AnalysisCard } from "@/features/analyses/components/AnalysisCard";
import { useAnalysesQuery, useDeleteAnalysisHistory } from "@/features/analyses/api/useAnalysisQuery";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { Brain, Trash2, ChevronLeft, ChevronRight, Filter } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import toast from "react-hot-toast";

export default function AnalysesPage() {
  const [selectedSourceId, setSelectedSourceId] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(0);
  const size = 10;

  const { data: sources } = useSourcesQuery();
  const { data: analysisData, isLoading } = useAnalysesQuery({ sourceId: selectedSourceId, page, size });
  const deleteHistory = useDeleteAnalysisHistory();

  const analyses = analysisData?.content || [];
  const totalPages = analysisData?.totalPages || 1;

  const handleClearHistory = () => {
    if (confirm("Seçili projenin analiz geçmişini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")) {
      deleteHistory.mutate(selectedSourceId, {
        onSuccess: () => {
          toast.success("Analiz geçmişi temizlendi.");
          setPage(0);
        },
        onError: () => {
          toast.error("Geçmiş temizlenirken bir hata oluştu.");
        }
      });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <PageHeader
          title="Analizler"
          description="Hata gruplarının kök neden analizleri ve detayları"
        />

        <div className="flex items-center gap-3">
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
            <select
              value={selectedSourceId || ""}
              onChange={(e) => {
                setSelectedSourceId(e.target.value ? Number(e.target.value) : undefined);
                setPage(0);
              }}
              className="pl-9 pr-8 py-2 bg-bg-secondary border border-border-default rounded-lg text-sm text-text-primary focus:outline-none focus:border-accent appearance-none cursor-pointer"
            >
              <option value="">Tüm Projeler</option>
              {sources?.map((source) => (
                <option key={source.id} value={source.id}>
                  {source.name}
                </option>
              ))}
            </select>
          </div>

          <Button 
            variant="danger" 
            onClick={handleClearHistory}
            disabled={deleteHistory.isPending || analyses.length === 0}
            className="flex items-center gap-2"
          >
            <Trash2 className="w-4 h-4" />
            <span className="hidden sm:inline">Geçmişi Temizle</span>
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : analyses.length > 0 ? (
        <div className="space-y-4">
          {analyses.map((analysis, index) => {
            const sourceName = sources?.find(s => s.id === analysis.sourceId)?.name;
            return (
              <AnalysisCard
                key={analysis.id}
                analysis={analysis}
                index={index}
                sourceName={sourceName}
              />
            );
          })}

          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t border-border-default mt-6">
              <span className="text-sm text-text-muted">
                Sayfa {page + 1} / {totalPages}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  <ChevronLeft className="w-4 h-4" />
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page === totalPages - 1}
                >
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </div>
            </div>
          )}
        </div>
      ) : (
        <EmptyState
          icon={<Brain size={28} />}
          title="Henüz analiz yok"
          description={selectedSourceId ? "Bu proje için henüz bir hata analizi yapılmamış." : "Sistemde henüz bir hata analizi bulunmuyor."}
        />
      )}
    </div>
  );
}
