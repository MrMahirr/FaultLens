"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface LoadingSpinnerProps {
  size?: "sm" | "md" | "lg";
  label?: string;
  fullScreen?: boolean;
  className?: string;
}

const sizeMap = { sm: "w-4 h-4", md: "w-6 h-6", lg: "w-9 h-9" };

/* ── Component (Instrument .spinner) ───────────────────────── */

function LoadingSpinner({
  size = "md",
  label,
  fullScreen = false,
  className,
}: LoadingSpinnerProps) {
  const t = useTranslations("common");
  const spinner = (
    <div className={cn("flex flex-col items-center gap-3", className)} role="status">
      <span className={cn("spinner text-text-muted", sizeMap[size])} aria-hidden="true" />
      <span className={label ? "small t3" : "sr"}>{label ?? t("loading")}</span>
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-bg-primary/80">
        {spinner}
      </div>
    );
  }

  return spinner;
}

export { LoadingSpinner, type LoadingSpinnerProps };
