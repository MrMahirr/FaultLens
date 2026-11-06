"use client";

import { type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

type BadgeVariant =
  "default" | "accent" | "success" | "warning" | "error" | "critical" | "info" | "ai" | "outline";
type BadgeSize = "sm" | "md" | "lg";

interface BadgeProps {
  variant?: BadgeVariant;
  size?: BadgeSize;
  children: ReactNode;
  icon?: ReactNode;
  /** Leading status dot — importance is never shown by colour alone. */
  dot?: boolean;
  pulse?: boolean;
  mono?: boolean;
  className?: string;
}

/* ── Style Maps (Instrument .badge + colour token) ─────────── */

const variantStyles: Record<BadgeVariant, string> = {
  default: "",
  accent: "c-signal",
  success: "c-ok",
  warning: "c-warn",
  error: "c-crit",
  critical: "c-crit",
  info: "c-info",
  ai: "c-ai",
  outline: "ol",
};

const sizeStyles: Record<BadgeSize, string> = {
  sm: "h-[18px] px-1.5 text-[10.5px]",
  md: "",
  lg: "h-6 px-2.5 text-[12.5px]",
};

/* ── Component ─────────────────────────────────────────────── */

function Badge({
  variant = "default",
  size = "md",
  children,
  icon,
  dot = false,
  pulse = false,
  mono = false,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "badge",
        variantStyles[variant],
        sizeStyles[size],
        mono && "mono",
        pulse && "blink",
        className,
      )}
    >
      {dot && <span className="d" aria-hidden="true" />}
      {icon && <span className="shrink-0 inline-flex">{icon}</span>}
      {children}
    </span>
  );
}

export { Badge, type BadgeProps, type BadgeVariant };
