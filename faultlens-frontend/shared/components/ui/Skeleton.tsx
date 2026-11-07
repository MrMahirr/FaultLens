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

/* ── Component (Instrument .skel — reserves space, CLS 0) ──── */

function Skeleton({ className, variant = "rectangular", width, height, lines }: SkeletonProps) {
  if (lines && lines > 1) {
    return (
      <div className={cn("space-y-2", className)} aria-hidden="true">
        {Array.from({ length: lines }, (_, i) => (
          <div key={i} className={cn("skel h-3.5", i === lines - 1 && "w-3/4")} />
        ))}
      </div>
    );
  }

  const variantStyles = {
    text: "h-3.5",
    circular: "rounded-full",
    rectangular: "rounded-md",
  };

  return (
    <div
      aria-hidden="true"
      className={cn("skel", variantStyles[variant], className)}
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
    <div className="card p-4 space-y-4" aria-hidden="true">
      <div className="flex items-center justify-between">
        <Skeleton variant="text" className="w-24" />
        <Skeleton variant="circular" width={28} height={28} />
      </div>
      <Skeleton variant="text" className="w-16 h-7" />
      <Skeleton lines={2} />
    </div>
  );
}

function SkeletonTableRow() {
  return (
    <div className="flex items-center gap-4 px-4 py-3 border-b border-border-default">
      <Skeleton variant="text" className="w-20" />
      <Skeleton variant="text" className="w-16" />
      <Skeleton variant="text" className="flex-1" />
      <Skeleton variant="text" className="w-24" />
    </div>
  );
}

function SkeletonTable({ rows = 5 }: { rows?: number }) {
  return (
    <div aria-hidden="true">
      {Array.from({ length: rows }, (_, i) => (
        <SkeletonTableRow key={i} />
      ))}
    </div>
  );
}

export { Skeleton, SkeletonCard, SkeletonTableRow, SkeletonTable, type SkeletonProps };
