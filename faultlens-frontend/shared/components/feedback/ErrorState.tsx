"use client";

import { AlertTriangle, RefreshCw } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/components/ui/Button";

interface ErrorStateProps {
  error?: (Error & { digest?: string }) | null;
  onRetry?: () => void;
  title?: string;
  description?: string;
}

/**
 * Page-level error (Instrument: `banner --crit` + retry). Used by route `error.tsx` files and
 * ErrorBoundary.
 */
export function ErrorState({ error, onRetry, title, description }: ErrorStateProps) {
  const t = useTranslations("errors");
  return (
    <div className="py-10 flex justify-center">
      <div className="banner c-crit w-full max-w-xl" role="alert">
        <AlertTriangle size={16} aria-hidden="true" />
        <div className="flex-1 min-w-0 space-y-1">
          <p className="font-medium">{title ?? t("pageTitle")}</p>
          <p className="small t2">{description ?? t("pageDescription")}</p>
          {error?.message && (
            <p className="mono xs t3 break-all">
              {error.message}
              {error.digest && ` · ${error.digest}`}
            </p>
          )}
          {onRetry && (
            <div className="pt-2">
              <Button
                variant="secondary"
                size="sm"
                icon={<RefreshCw size={14} aria-hidden="true" />}
                onClick={onRetry}
              >
                {t("retry")}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
