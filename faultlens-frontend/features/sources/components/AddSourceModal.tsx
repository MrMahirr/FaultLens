"use client";

import { AnimatePresence } from "framer-motion";
import { Modal } from "@/shared/components/ui/Modal";
import { useSourceWizard } from "../hooks/useSourceWizard";
import { SourceTypeSelectionStep } from "./SourceTypeSelectionStep";
import { SourceConfigFormStep } from "./SourceConfigFormStep";
import { SourceTestResultStep } from "./SourceTestResultStep";

interface AddSourceModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function AddSourceModal({ isOpen, onClose }: AddSourceModalProps) {
  const wizard = useSourceWizard(onClose);

  return (
    <Modal
      isOpen={isOpen}
      onClose={wizard.handleClose}
      title="Yeni Kaynak Ekle"
      description={`Adım ${wizard.step}/3`}
      size="lg"
    >
      <AnimatePresence mode="wait">
        {wizard.step === 1 && (
          <SourceTypeSelectionStep 
            selectedType={wizard.selectedType}
            onSelect={wizard.handleTypeSelect}
          />
        )}

        {wizard.step === 2 && (
          <SourceConfigFormStep
            selectedType={wizard.selectedType}
            name={wizard.name} setName={wizard.setName}
            host={wizard.host} setHost={wizard.setHost}
            namespace={wizard.namespace} setNamespace={wizard.setNamespace}
            username={wizard.username} setUsername={wizard.setUsername}
            password={wizard.password} setPassword={wizard.setPassword}
            logFilePath={wizard.logFilePath} setLogFilePath={wizard.setLogFilePath}
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
      </AnimatePresence>
    </Modal>
  );
}
