"use client";

import { Suspense } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Brain, Trash2 } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { Button } from "@/shared/components/ui/Button";
import { DropdownSelect } from "@/shared/components/ui/Dropdown";
import { Pagination } from "@/shared/components/ui/Pagination";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { useUrlState } from "@/shared/hooks/useUrlState";
import { AnalysisCard } from "@/features/analyses/components/AnalysisCard";
import {
  useAnalysesQuery,
  useDeleteAnalysisHistory,
} from "@/features/analyses/api/useAnalysisQuery";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";

const PAGE_SIZE = 10;

function AnalysesView() {
  const t = useTranslations("analyses");
  // Filters live in the URL (?source=3&page=2) so the view can be shared as a link.
  const { params, update } = useUrlState();
  const sourceParam = Number(params.get("source"));
  const sourceId = Number.isInteger(sourceParam) && sourceParam > 0 ? sourceParam : undefined;
  const page = Math.max(0, (Number(params.get("page")) || 1) - 1);

  const { data: sources } = useSourcesQuery();
  const { data: analysisData, isLoading } = useAnalysesQuery({ sourceId, page, size: PAGE_SIZE });
  const deleteHistory = useDeleteAnalysisHistory();
  const canDelete = useCan("delete");
  const { confirm, dialog } = useConfirm();

  const analyses = analysisData?.content ?? [];
  const totalPages = analysisData?.totalPages ?? 1;
  const sourceName = (id?: number) => sources?.find((s) => s.id === id)?.name;

  const handleClearHistory = () =>
    confirm({
      title: t("clearTitle"),
      description: sourceId
        ? t("clearDescriptionSource", { name: sourceName(sourceId) ?? `#${sourceId}` })
        : t("clearDescriptionAll"),
      onConfirm: () =>
        deleteHistory.mutateAsync(sourceId).then(
          () => {
            toast.success(t("cleared"));
            update({ page: undefined });
          },
          (error: unknown) => {
            toast.error(t("clearFailed"));
            throw error;
          },
        ),
    });

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        action={
          <>
            <DropdownSelect
              label={t("source")}
              value={sourceId ? String(sourceId) : ""}
              onChange={(value) => update({ source: value, page: undefined })}
              options={[
                { label: t("allSources"), value: "" },
                ...(sources ?? []).map((s) => ({ label: s.name, value: String(s.id) })),
              ]}
              className="min-w-[180px] [&_.label]:sr-only"
            />
            {canDelete && (
              <Button
                variant="danger"
                onClick={handleClearHistory}
                disabled={deleteHistory.isPending || analyses.length === 0}
                icon={<Trash2 size={14} aria-hidden="true" />}
                aria-label={t("clearHistory")}
              >
                <span className="hidden sm:inline">{t("clearHistory")}</span>
              </Button>
            )}
          </>
        }
      />

      {isLoading ? (
        <div className="stack" aria-busy="true">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : analyses.length > 0 ? (
        <>
          <ul className="stack p-0 m-0" aria-label={t("list")}>
            {analyses.map((analysis, index) => (
              <AnalysisCard
                key={analysis.id}
                analysis={analysis}
                index={index}
                sourceName={sourceName(analysis.sourceId)}
              />
            ))}
          </ul>
          {totalPages > 1 && (
            <div className="flex justify-center pt-2">
              <Pagination
                currentPage={page + 1}
                totalPages={totalPages}
                onPageChange={(p) => update({ page: p > 1 ? p : undefined })}
              />
            </div>
          )}
        </>
      ) : (
        <div className="card">
          <EmptyState
            icon={<Brain size={18} />}
            title={t("emptyTitle")}
            description={sourceId ? t("emptySource") : t("emptyAll")}
          />
        </div>
      )}
      {dialog}
    </>
  );
}

export default function AnalysesPage() {
  // useSearchParams needs a Suspense boundary.
  return (
    <Suspense>
      <AnalysesView />
    </Suspense>
  );
}
