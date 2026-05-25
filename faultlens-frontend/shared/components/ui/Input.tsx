"use client";

import { forwardRef, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  icon?: ReactNode;
  iconPosition?: "left" | "right";
  fullWidth?: boolean;
}

/* ── Component ─────────────────────────────────────────────── */

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      hint,
      icon,
      iconPosition = "left",
      fullWidth = true,
      className,
      id,
      ...props
    },
    ref
  ) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s+/g, "-");

    return (
      <div className={cn("flex flex-col gap-1.5", fullWidth && "w-full")}>
        {label && (
          <label
            htmlFor={inputId}
            className="text-sm font-medium text-text-secondary"
          >
            {label}
          </label>
        )}

        <div className="relative">
          {icon && iconPosition === "left" && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted">
              {icon}
            </span>
          )}

          <input
            ref={ref}
            id={inputId}
            className={cn(
              "w-full bg-bg-tertiary text-text-primary rounded-lg",
              "border border-border-default",
              "placeholder:text-text-muted",
              "transition-all duration-200",
              "focus:outline-none focus:border-border-active focus:ring-1 focus:ring-accent/30",
              "disabled:opacity-50 disabled:cursor-not-allowed",
              "px-3 py-2 text-sm",
              icon && iconPosition === "left" && "pl-10",
              icon && iconPosition === "right" && "pr-10",
              error && "border-error focus:border-error focus:ring-error/30",
              className
            )}
            {...props}
          />

          {icon && iconPosition === "right" && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted">
              {icon}
            </span>
          )}
        </div>

        {error && (
          <p className="text-xs text-error flex items-center gap-1">
            <span className="inline-block w-1 h-1 rounded-full bg-error" />
            {error}
          </p>
        )}

        {hint && !error && (
          <p className="text-xs text-text-muted">{hint}</p>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";

export { Input, type InputProps };
