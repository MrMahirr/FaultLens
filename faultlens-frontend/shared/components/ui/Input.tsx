"use client";

import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from "react";
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

/* ── Component (Instrument .field / .input) ────────────────── */

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    { label, error, hint, icon, iconPosition = "left", fullWidth = true, className, id, ...props },
    ref,
  ) => {
    const autoId = useId();
    const inputId = id ?? autoId;
    const errorId = `${inputId}-error`;
    const hintId = `${inputId}-hint`;

    return (
      <div className={cn("field", fullWidth && "w-full")}>
        {label && (
          <label htmlFor={inputId} className="label">
            {label}
          </label>
        )}

        <div className="relative">
          {icon && iconPosition === "left" && (
            <span
              className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted inline-flex"
              aria-hidden="true"
            >
              {icon}
            </span>
          )}

          <input
            ref={ref}
            id={inputId}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? errorId : hint ? hintId : undefined}
            className={cn(
              "input",
              icon && iconPosition === "left" && "pl-9",
              icon && iconPosition === "right" && "pr-9",
              error && "is-error",
              "disabled:opacity-50 disabled:cursor-not-allowed",
              className,
            )}
            {...props}
          />

          {icon && iconPosition === "right" && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted inline-flex">
              {icon}
            </span>
          )}
        </div>

        {error && (
          <p id={errorId} className="err" role="alert">
            {error}
          </p>
        )}

        {hint && !error && (
          <p id={hintId} className="help">
            {hint}
          </p>
        )}
      </div>
    );
  },
);

Input.displayName = "Input";

export { Input, type InputProps };
