"use client";

import { useTranslations } from "next-intl";
import { CheckCircle2, XCircle, ArrowLeft } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { LogSourceType } from "@/shared/types/common.types";

interface SourceTestResultStepProps {
  name: string;
  selectedType: LogSourceType | null;
  testResult: "success" | "error" | null;
  isTesting: boolean;
  isSaving: boolean;
  onTest: () => void;
  onBack: () => void;
  onSave: () => void;
}

export function SourceTestResultStep({
  name,
  selectedType,
  testResult,
  isTesting,
  isSaving,
  onTest,
  onBack,
  onSave,
}: SourceTestResultStepProps) {
  const t = useTranslations("sources.wizard");
  const tSources = useTranslations("sources");

  return (
    <div className="stack gap-4 swA">
      <dl className="kv well p-3">
        <dt>{t("source")}</dt>
        <dd className="mono">{name}</dd>
        {selectedType && (
          <>
            <dt>{t("chooseType")}</dt>
            <dd>{tSources(`types.${selectedType}`)}</dd>
          </>
        )}
      </dl>

      <div className="flex flex-col items-center gap-3 py-4" aria-live="polite">
        {testResult === null && !isTesting && (
          <Button variant="secondary" onClick={onTest}>
            {t("test")}
          </Button>
        )}

        {isTesting && (
          <p className="row small t2" role="status">
            <span className="spinner" aria-hidden="true" />
            {t("testing")}
          </p>
        )}

        {testResult === "success" && (
          <p className="row small text-success font-medium">
            <CheckCircle2 size={18} aria-hidden="true" />
            {t("testOk")}
          </p>
        )}

        {testResult === "error" && (
          <div className="stack items-center gap-2">
            <p className="row small text-error font-medium">
              <XCircle size={18} aria-hidden="true" />
              {t("testFailed")}
            </p>
            <Button variant="ghost" size="sm" onClick={onTest}>
              {t("test")}
            </Button>
          </div>
        )}
      </div>

      <div className="flex gap-2">
        <Button
          variant="secondary"
          icon={<ArrowLeft size={14} aria-hidden="true" />}
          onClick={onBack}
        >
          {t("back")}
        </Button>
        <Button
          variant="primary"
          fullWidth
          onClick={onSave}
          loading={isSaving}
          disabled={testResult !== "success"}
          title={testResult !== "success" ? t("saveHint") : undefined}
        >
          {t("save")}
        </Button>
      </div>
    </div>
  );
}
