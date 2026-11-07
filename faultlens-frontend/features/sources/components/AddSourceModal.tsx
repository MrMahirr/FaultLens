"use client";

import { useTranslations } from "next-intl";
import { Modal } from "@/shared/components/ui/Modal";
import { useSourceWizard } from "../hooks/useSourceWizard";
import { SourceTypeSelectionStep } from "./SourceTypeSelectionStep";
import { SourceConfigFormStep } from "./SourceConfigFormStep";
import { SourceTestResultStep } from "./SourceTestResultStep";
import { IngestSetup } from "./IngestSetup";
import { Button } from "@/shared/components/ui/Button";
import { LogSourceType } from "@/shared/types/common.types";

interface AddSourceModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function AddSourceModal({ isOpen, onClose }: AddSourceModalProps) {
  const t = useTranslations("sources.wizard");
  const tIngest = useTranslations("sources.ingest");
  const wizard = useSourceWizard(onClose);

  return (
    <Modal
      isOpen={isOpen}
      onClose={wizard.handleClose}
      title={t("title")}
      description={`${t("step", { step: wizard.step })} · ${t(`steps.${wizard.step as 1 | 2 | 3 | 4}`)}`}
      size="lg"
    >
      {/* Progress (Instrument .steps) */}
      <div className="steps" aria-hidden="true">
        {(wizard.selectedType === LogSourceType.HTTP ? [1, 2, 3, 4] : [1, 2, 3]).map((n) => (
          <i key={n} style={{ ["--k" as string]: wizard.step >= n ? 1 : 0 }} />
        ))}
      </div>

      {wizard.step === 1 && (
        <SourceTypeSelectionStep
          selectedType={wizard.selectedType}
          onSelect={wizard.handleTypeSelect}
        />
      )}

      {wizard.step === 2 && (
        <SourceConfigFormStep
          selectedType={wizard.selectedType}
          name={wizard.name}
          setName={wizard.setName}
          host={wizard.host}
          setHost={wizard.setHost}
          namespace={wizard.namespace}
          setNamespace={wizard.setNamespace}
          username={wizard.username}
          setUsername={wizard.setUsername}
          password={wizard.password}
          setPassword={wizard.setPassword}
          logFilePath={wizard.logFilePath}
          setLogFilePath={wizard.setLogFilePath}
          onBack={() => wizard.setStep(1)}
          onNext={() => wizard.setStep(3)}
        />
      )}

      {wizard.step === 3 && (
        <SourceTestResultStep
          name={wizard.name}
          selectedType={wizard.selectedType}
          testResult={wizard.testResult}
          isTesting={wizard.isTesting}
          isSaving={wizard.isSaving}
          onTest={wizard.handleTest}
          onBack={() => {
            wizard.setStep(2);
            wizard.setTestResult(null);
          }}
          onSave={wizard.handleSave}
        />
      )}

      {wizard.step === 4 && wizard.createdSource && (
        <>
          <IngestSetup sourceId={wizard.createdSource.id} sourceName={wizard.createdSource.name} />
          <div className="flex justify-end">
            <Button variant="primary" onClick={wizard.handleClose}>
              {tIngest("done")}
            </Button>
          </div>
        </>
      )}
    </Modal>
  );
}
