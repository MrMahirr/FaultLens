"use client";

import { useMemo, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
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
  siblings: number
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
      (_, i) => total - rightItemCount + i + 1
    );
    return [1, "dots", ...rightRange];
  }

  const middleRange = Array.from(
    { length: rightSiblingIndex - leftSiblingIndex + 1 },
    (_, i) => leftSiblingIndex + i
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
  const pages = useMemo(
    () => generatePageNumbers(currentPage, totalPages, siblingCount),
    [currentPage, totalPages, siblingCount]
  );

  const handlePrev = useCallback(() => {
    if (currentPage > 1) onPageChange(currentPage - 1);
  }, [currentPage, onPageChange]);

  const handleNext = useCallback(() => {
    if (currentPage < totalPages) onPageChange(currentPage + 1);
  }, [currentPage, totalPages, onPageChange]);

  if (totalPages <= 1) return null;

  return (
    <nav
      role="navigation"
      aria-label="Sayfalama"
      className={cn("flex items-center gap-1", className)}
    >
      <button
        onClick={handlePrev}
        disabled={currentPage === 1}
        className="p-2 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        aria-label="Önceki sayfa"
      >
        <ChevronLeft size={16} />
      </button>

      {pages.map((page, idx) => {
        if (page === "dots") {
          return (
            <span
              key={`dots-${idx}`}
              className="px-2 py-1 text-text-muted text-sm"
            >
              …
            </span>
          );
        }

        return (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={cn(
              "min-w-[32px] h-8 rounded-lg text-sm font-medium transition-all duration-200",
              page === currentPage
                ? "bg-accent text-white shadow-glow"
                : "text-text-secondary hover:text-text-primary hover:bg-bg-tertiary"
            )}
            aria-current={page === currentPage ? "page" : undefined}
          >
            {page}
          </button>
        );
      })}

      <button
        onClick={handleNext}
        disabled={currentPage === totalPages}
        className="p-2 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        aria-label="Sonraki sayfa"
      >
        <ChevronRight size={16} />
      </button>
    </nav>
  );
}

export { Pagination, type PaginationProps };
