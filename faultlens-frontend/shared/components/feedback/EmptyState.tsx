"use client";

import { type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";
import { Inbox } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";

/* ── Types ─────────────────────────────────────────────────── */

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

/* ── Component ─────────────────────────────────────────────── */

function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center py-16 px-4 text-center",
        className
      )}
    >
      <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-bg-tertiary text-text-muted mb-4">
        {icon ?? <Inbox size={28} />}
      </div>

      <h3 className="text-lg font-semibold font-display text-text-primary mb-1">
        {title}
      </h3>

      {description && (
        <p className="text-sm text-text-secondary max-w-sm mb-4">
          {description}
        </p>
      )}

      {action && (
        <Button variant="primary" size="sm" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </div>
  );
}

export { EmptyState, type EmptyStateProps };
