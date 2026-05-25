"use client";

import { type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

type BadgeVariant =
  | "default"
  | "accent"
  | "success"
  | "warning"
  | "error"
  | "critical"
  | "info"
  | "outline";
type BadgeSize = "sm" | "md" | "lg";

interface BadgeProps {
  variant?: BadgeVariant;
  size?: BadgeSize;
  children: ReactNode;
  icon?: ReactNode;
  pulse?: boolean;
  className?: string;
}

/* ── Style Maps ────────────────────────────────────────────── */

const variantStyles: Record<BadgeVariant, string> = {
  default: "bg-bg-tertiary text-text-secondary",
  accent: "bg-accent/10 text-accent border border-accent/20",
  success: "bg-success/10 text-success border border-success/20",
  warning: "bg-warning/10 text-warning border border-warning/20",
  error: "bg-error/10 text-error border border-error/20",
  critical: "bg-critical/15 text-critical border border-critical/20",
  info: "bg-severity-info/10 text-severity-info border border-severity-info/20",
  outline: "bg-transparent text-text-secondary border border-border-default",
};

const sizeStyles: Record<BadgeSize, string> = {
  sm: "px-1.5 py-0.5 text-[10px]",
  md: "px-2 py-0.5 text-xs",
  lg: "px-3 py-1 text-sm",
};

/* ── Component ─────────────────────────────────────────────── */

function Badge({
  variant = "default",
  size = "md",
  children,
  icon,
  pulse = false,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 font-medium rounded-full font-mono",
        "transition-colors duration-200",
        variantStyles[variant],
        sizeStyles[size],
        pulse && "animate-glow-pulse",
        className
      )}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      {children}
    </span>
  );
}

export { Badge, type BadgeProps, type BadgeVariant, type BadgeSize };
