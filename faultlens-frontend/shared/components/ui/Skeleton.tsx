"use client";

import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface SkeletonProps {
  className?: string;
  variant?: "text" | "circular" | "rectangular";
  width?: string | number;
  height?: string | number;
  lines?: number;
}

/* ── Component ─────────────────────────────────────────────── */

function Skeleton({
  className,
  variant = "rectangular",
  width,
  height,
  lines,
}: SkeletonProps) {
  const baseStyles = "animate-pulse bg-bg-tertiary";

  // Multiple lines of text skeleton
  if (lines && lines > 1) {
    return (
      <div className={cn("space-y-2", className)}>
        {Array.from({ length: lines }, (_, i) => (
          <div
            key={i}
            className={cn(
              baseStyles,
              "h-4 rounded",
              i === lines - 1 && "w-3/4"
            )}
          />
        ))}
      </div>
    );
  }

  const variantStyles = {
    text: "h-4 rounded",
    circular: "rounded-full",
    rectangular: "rounded-lg",
  };

  return (
    <div
      className={cn(baseStyles, variantStyles[variant], className)}
      style={{
        width: typeof width === "number" ? `${width}px` : width,
        height: typeof height === "number" ? `${height}px` : height,
      }}
    />
  );
}

/* ── Preset Skeletons ──────────────────────────────────────── */

function SkeletonCard() {
  return (
    <div className="bg-bg-secondary border border-border-default rounded-xl p-5 space-y-4">
      <div className="flex items-center justify-between">
        <Skeleton variant="text" className="w-24" />
        <Skeleton variant="circular" width={32} height={32} />
      </div>
      <Skeleton variant="text" className="w-16 h-8" />
      <Skeleton lines={2} />
    </div>
  );
}

function SkeletonTableRow() {
  return (
    <div className="flex items-center gap-4 p-3 border-b border-border-default">
      <Skeleton variant="text" className="w-20" />
      <Skeleton variant="text" className="w-16" />
      <Skeleton variant="text" className="flex-1" />
      <Skeleton variant="text" className="w-24" />
    </div>
  );
}

function SkeletonTable({ rows = 5 }: { rows?: number }) {
  return (
    <div>
      {Array.from({ length: rows }, (_, i) => (
        <SkeletonTableRow key={i} />
      ))}
    </div>
  );
}

export {
  Skeleton,
  SkeletonCard,
  SkeletonTableRow,
  SkeletonTable,
  type SkeletonProps,
};
