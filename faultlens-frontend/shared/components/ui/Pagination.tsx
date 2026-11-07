"use client";

import { useMemo, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  siblingCount?: number;
  className?: string;
}

/* ── Helpers ───────────────────────────────────────────────── */

function generatePageNumbers(
  current: number,
  total: number,
  siblings: number,
): (number | "dots")[] {
  const totalNumbers = siblings * 2 + 3; // siblings + current + first + last
  const totalBlocks = totalNumbers + 2; // + 2 dots

  if (total <= totalBlocks) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  const leftSiblingIndex = Math.max(current - siblings, 1);
  const rightSiblingIndex = Math.min(current + siblings, total);

  const showLeftDots = leftSiblingIndex > 2;
  const showRightDots = rightSiblingIndex < total - 1;

  if (!showLeftDots && showRightDots) {
    const leftItemCount = 3 + 2 * siblings;
    const leftRange = Array.from({ length: leftItemCount }, (_, i) => i + 1);
    return [...leftRange, "dots", total];
  }

  if (showLeftDots && !showRightDots) {
    const rightItemCount = 3 + 2 * siblings;
    const rightRange = Array.from(
      { length: rightItemCount },
      (_, i) => total - rightItemCount + i + 1,
    );
    return [1, "dots", ...rightRange];
  }

  const middleRange = Array.from(
    { length: rightSiblingIndex - leftSiblingIndex + 1 },
    (_, i) => leftSiblingIndex + i,
  );
  return [1, "dots", ...middleRange, "dots", total];
}

/* ── Component ─────────────────────────────────────────────── */

function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  siblingCount = 1,
  className,
}: PaginationProps) {
  const t = useTranslations("common");
  const pages = useMemo(
    () => generatePageNumbers(currentPage, totalPages, siblingCount),
    [currentPage, totalPages, siblingCount],
  );

  const handlePrev = useCallback(() => {
    if (currentPage > 1) onPageChange(currentPage - 1);
  }, [currentPage, onPageChange]);

  const handleNext = useCallback(() => {
    if (currentPage < totalPages) onPageChange(currentPage + 1);
  }, [currentPage, totalPages, onPageChange]);

  if (totalPages <= 1) return null;

  return (
    <nav aria-label={t("pagination")} className={cn("flex items-center gap-1", className)}>
      <button
        type="button"
        onClick={handlePrev}
        disabled={currentPage === 1}
        className="btn btn-ghost btn-sm btn-icon"
        aria-label={t("previousPage")}
      >
        <ChevronLeft size={16} aria-hidden="true" />
      </button>

      {pages.map((page, idx) => {
        if (page === "dots") {
          return (
            <span
              key={`dots-${idx}`}
              className="px-2 py-1 text-text-muted text-sm"
              aria-hidden="true"
            >
              …
            </span>
          );
        }

        return (
          <button
            key={page}
            type="button"
            onClick={() => onPageChange(page)}
            aria-label={t("page", { page })}
            className={cn("btn btn-sm num", page === currentPage ? "btn-primary" : "btn-ghost")}
            aria-current={page === currentPage ? "page" : undefined}
          >
            {page}
          </button>
        );
      })}

      <button
        type="button"
        onClick={handleNext}
        disabled={currentPage === totalPages}
        className="btn btn-ghost btn-sm btn-icon"
        aria-label={t("nextPage")}
      >
        <ChevronRight size={16} aria-hidden="true" />
      </button>
    </nav>
  );
}

export { Pagination, type PaginationProps };
