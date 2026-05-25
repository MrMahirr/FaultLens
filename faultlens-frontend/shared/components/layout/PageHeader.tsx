import { type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

/* ── Component ─────────────────────────────────────────────── */

function PageHeader({ title, description, action, className }: PageHeaderProps) {
  return (
    <div
      className={cn(
        "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6",
        className
      )}
    >
      <div>
        <h1 className="text-2xl font-bold font-display text-text-primary">
          {title}
        </h1>
        {description && (
          <p className="mt-1 text-sm text-text-secondary">{description}</p>
        )}
      </div>

      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

export { PageHeader, type PageHeaderProps };
