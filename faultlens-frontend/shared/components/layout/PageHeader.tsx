import { type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

/* ── Component (Instrument .page-head) ─────────────────────── */

function PageHeader({ title, description, action, className }: PageHeaderProps) {
  return (
    <div className={cn("page-head mb-5", className)}>
      <div className="min-w-0">
        <h1 className="h1">{title}</h1>
        {description && <p className="mt-1.5 small t3">{description}</p>}
      </div>

      {action && <div className="shrink-0 flex flex-wrap items-center gap-2">{action}</div>}
    </div>
  );
}

export { PageHeader, type PageHeaderProps };
