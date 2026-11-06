"use client";

import { type ReactNode } from "react";
import { Inbox } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/components/ui/Button";

/* ── Types ─────────────────────────────────────────────────── */

interface EmptyStateProps {
  icon?: ReactNode;
  /** Why it is empty. */
  title: string;
  /** What to do next ("Widen the filter", "Add a source"). */
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

/* ── Component (Instrument .empty) ─────────────────────────── */

function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn("empty", className)} role="status">
      <div
        className="flex items-center justify-center w-10 h-10 rounded-md well text-text-muted mb-1"
        aria-hidden="true"
      >
        {icon ?? <Inbox size={18} />}
      </div>
      <h3 className="h3 text-text-primary">{title}</h3>
      {description && <p className="small max-w-sm">{description}</p>}
      {action && (
        <Button variant="secondary" size="sm" onClick={action.onClick} className="mt-2">
          {action.label}
        </Button>
      )}
    </div>
  );
}

export { EmptyState, type EmptyStateProps };
