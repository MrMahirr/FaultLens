import { useTranslations } from "next-intl";
import { SkeletonCard, SkeletonTable } from "@/shared/components/ui/Skeleton";

/**
 * Shared body of the routes' loading.tsx: reserves the page header and content area
 * (no layout shift) instead of a spinner.
 */
export function RouteLoading({ variant = "cards" }: { variant?: "cards" | "table" | "detail" }) {
  const t = useTranslations("common");
  return (
    <div className="stack gap-5" role="status" aria-busy="true">
      {/* The skeletons are aria-hidden; this is what a screen reader announces. */}
      <span className="sr">{t("loading")}</span>
      <div className="stack gap-2" aria-hidden="true">
        <div className="skel h-8 w-56" />
        <div className="skel h-4 w-80 max-w-full" />
      </div>
      {variant === "table" ? (
        <div className="card">
          <SkeletonTable rows={10} />
        </div>
      ) : variant === "detail" ? (
        <div className="stack" aria-hidden="true">
          <div className="skel h-24" />
          <div className="skel h-48" />
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {Array.from({ length: 6 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      )}
    </div>
  );
}
