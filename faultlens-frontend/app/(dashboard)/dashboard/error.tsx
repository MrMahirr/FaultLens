"use client";

import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[50vh] p-8 text-center">
      <div className="w-16 h-16 rounded-2xl bg-error/10 flex items-center justify-center mb-4">
        <AlertTriangle size={28} className="text-error" />
      </div>
      <h2 className="text-xl font-bold font-display text-text-primary mb-2">
        Dashboard yüklenemedi
      </h2>
      <p className="text-sm text-text-secondary max-w-md mb-4">
        {error.message || "Beklenmeyen bir hata oluştu."}
      </p>
      <Button
        variant="secondary"
        icon={<RefreshCw size={16} />}
        onClick={reset}
      >
        Tekrar Dene
      </Button>
    </div>
  );
}
