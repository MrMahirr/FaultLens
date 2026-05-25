"use client";

import { type ReactNode, type HTMLAttributes } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

type CardVariant = "default" | "glass" | "bordered" | "flat";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: CardVariant;
  padding?: "none" | "sm" | "md" | "lg";
  hover?: boolean;
  glow?: boolean;
  children: ReactNode;
}

/* ── Style Maps ────────────────────────────────────────────── */

const variantStyles: Record<CardVariant, string> = {
  default: "bg-bg-secondary border border-border-default",
  glass: "glass",
  bordered: "bg-transparent border border-border-default",
  flat: "bg-bg-secondary",
};

const paddingStyles: Record<string, string> = {
  none: "p-0",
  sm: "p-3",
  md: "p-4 sm:p-5",
  lg: "p-6 sm:p-8",
};

/* ── Component ─────────────────────────────────────────────── */

function Card({
  variant = "default",
  padding = "md",
  hover = false,
  glow = false,
  children,
  className,
  ...props
}: CardProps) {
  return (
    <div
      className={cn(
        "rounded-xl transition-all duration-300",
        variantStyles[variant],
        paddingStyles[padding],
        hover &&
          "hover:-translate-y-0.5 hover:border-border-hover hover:shadow-md cursor-pointer",
        glow && "hover:shadow-glow",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export { Card, type CardProps, type CardVariant };
