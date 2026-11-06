"use client";

import { type ReactNode, type HTMLAttributes } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

type CardVariant = "default" | "glass" | "bordered" | "flat";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: CardVariant;
  padding?: "none" | "sm" | "md" | "lg";
  hover?: boolean;
  /** Kept for API compatibility; Instrument has no glow (hairlines, not shadows). */
  glow?: boolean;
  children: ReactNode;
}

/* ── Style Maps (Instrument .card / .well) ─────────────────── */

const variantStyles: Record<CardVariant, string> = {
  default: "card",
  glass: "card",
  bordered: "card bg-transparent",
  flat: "well",
};

const paddingStyles: Record<NonNullable<CardProps["padding"]>, string> = {
  none: "p-0",
  sm: "p-3",
  md: "p-4",
  lg: "p-6",
};

/* ── Component ─────────────────────────────────────────────── */

function Card({
  variant = "default",
  padding = "md",
  hover = false,
  glow,
  children,
  className,
  ...props
}: CardProps) {
  void glow; // accepted for compatibility, intentionally unused
  return (
    <div
      className={cn(
        variantStyles[variant],
        paddingStyles[padding],
        hover && "hov cursor-pointer",
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export { Card, type CardProps, type CardVariant };
