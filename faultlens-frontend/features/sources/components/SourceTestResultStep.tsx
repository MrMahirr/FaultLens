import { motion } from "framer-motion";
import { CheckCircle2, XCircle, ArrowLeft } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
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
  name, selectedType,
  testResult, isTesting, isSaving,
  onTest, onBack, onSave
}: SourceTestResultStepProps) {
  return (
    <motion.div
      key="step3"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      className="space-y-4"
    >
      <Card variant="bordered" padding="md">
        <p className="text-sm text-text-secondary mb-1">Kaynak:</p>
        <p className="font-mono text-text-primary">{name}</p>
        <p className="text-xs text-text-muted mt-1">
          Tür: {selectedType}
        </p>
      </Card>

      <div className="flex flex-col items-center gap-4 py-4">
        {testResult === null && !isTesting && (
          <Button variant="primary" onClick={onTest}>
            Bağlantıyı Test Et
          </Button>
        )}

        {isTesting && (
          <div className="flex items-center gap-2 text-text-secondary">
            <div className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
            <span className="text-sm">Bağlantı analiz ediliyor...</span>
          </div>
        )}

        {testResult === "success" && (
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            className="flex flex-col items-center gap-2"
          >
            <CheckCircle2 size={48} className="text-success" />
            <p className="text-sm text-success font-medium">
              Bağlantı başarılı!
            </p>
          </motion.div>
        )}

        {testResult === "error" && (
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            className="flex flex-col items-center gap-2"
          >
            <XCircle size={48} className="text-error" />
            <p className="text-sm text-error font-medium">
              Bağlantı başarısız
            </p>
          </motion.div>
        )}
      </div>

      <div className="flex gap-2">
        <Button
          variant="secondary"
          icon={<ArrowLeft size={14} />}
          onClick={onBack}
        >
          Geri
        </Button>
        <Button
          variant="success"
          fullWidth
          onClick={onSave}
          loading={isSaving}
          disabled={testResult !== "success"}
        >
          Kaydet
        </Button>
      </div>
    </motion.div>
  );
}
