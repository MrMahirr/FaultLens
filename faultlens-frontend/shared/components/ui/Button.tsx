"use client";

import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";
import { Loader2 } from "lucide-react";

/* ── Types ─────────────────────────────────────────────────── */

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger" | "success";
type ButtonSize = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  icon?: ReactNode;
  iconPosition?: "left" | "right";
  fullWidth?: boolean;
}

/* ── Style Maps ────────────────────────────────────────────── */

const variantStyles: Record<ButtonVariant, string> = {
  primary:
    "bg-accent hover:bg-accent-hover text-white shadow-glow hover:shadow-lg",
  secondary:
    "bg-bg-tertiary hover:bg-bg-glass-hover text-text-primary border border-border-default hover:border-border-hover",
  ghost:
    "bg-transparent hover:bg-bg-tertiary text-text-secondary hover:text-text-primary",
  danger:
    "bg-error/10 hover:bg-error/20 text-error border border-error/20 hover:border-error/40",
  success:
    "bg-success/10 hover:bg-success/20 text-success border border-success/20 hover:border-success/40",
};

const sizeStyles: Record<ButtonSize, string> = {
  sm: "px-3 py-1.5 text-xs gap-1.5",
  md: "px-4 py-2 text-sm gap-2",
  lg: "px-6 py-2.5 text-base gap-2.5",
};

/* ── Component ─────────────────────────────────────────────── */

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = "primary",
      size = "md",
      loading = false,
      icon,
      iconPosition = "left",
      fullWidth = false,
      disabled,
      className,
      children,
      ...props
    },
    ref
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        ref={ref}
        disabled={isDisabled}
        className={cn(
          "inline-flex items-center justify-center font-medium rounded-lg",
          "transition-all duration-200 ease-out cursor-pointer",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent",
          "disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none",
          variantStyles[variant],
          sizeStyles[size],
          fullWidth && "w-full",
          className
        )}
        {...props}
      >
        {loading ? (
          <Loader2 className="animate-spin" size={size === "sm" ? 14 : 16} />
        ) : (
          <>
            {icon && iconPosition === "left" && (
              <span className="shrink-0">{icon}</span>
            )}
            {children}
            {icon && iconPosition === "right" && (
              <span className="shrink-0">{icon}</span>
            )}
          </>
        )}
      </button>
    );
  }
);

Button.displayName = "Button";

export { Button, type ButtonProps, type ButtonVariant, type ButtonSize };
