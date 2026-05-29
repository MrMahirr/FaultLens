"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Server, Terminal, Container, CheckCircle2, XCircle, ArrowRight, ArrowLeft } from "lucide-react";
import { Modal } from "@/shared/components/ui/Modal";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { Card } from "@/shared/components/ui/Card";
import { LogSourceType } from "@/shared/types/common.types";
import { cn } from "@/shared/lib/utils";
import { useTestConnectionMutation, useCreateSourceMutation } from "@/features/sources/api/useSourceMutation";

/* ── Props ─────────────────────────────────────────────────── */

interface AddSourceModalProps {
  isOpen: boolean;
  onClose: () => void;
}

/* ── Source Type Options ───────────────────────────────────── */

const SOURCE_TYPES = [
  {
    type: LogSourceType.KUBERNETES,
    label: "Kubernetes",
    icon: <Server size={28} />,
    description: "K8s cluster loglarını izleyin",
  },
  {
    type: LogSourceType.SSH,
    label: "SSH",
    icon: <Terminal size={28} />,
    description: "Uzak sunucu loglarını izleyin",
  },
  {
    type: LogSourceType.DOCKER,
    label: "Docker",
    icon: <Container size={28} />,
    description: "Docker container loglarını izleyin",
  },
];

/* ── Component ─────────────────────────────────────────────── */

function AddSourceModal({ isOpen, onClose }: AddSourceModalProps) {
  const [step, setStep] = useState(1);
  const [selectedType, setSelectedType] = useState<LogSourceType | null>(null);
  const [name, setName] = useState("");
  const [host, setHost] = useState("");
  const [namespace, setNamespace] = useState("");
  const [testResult, setTestResult] = useState<"success" | "error" | null>(null);

  const testConnection = useTestConnectionMutation();
  const createSource = useCreateSourceMutation();

  const handleTypeSelect = (type: LogSourceType) => {
    setSelectedType(type);
    setStep(2);
  };

  const handleTest = async () => {
    setTestResult(null);
    try {
      await testConnection.mutateAsync(0);
      setTestResult("success");
    } catch {
      setTestResult("error");
    }
  };

  const handleSave = async () => {
    await createSource.mutateAsync({
      name,
      type: selectedType!,
      host: host || undefined,
      namespace: namespace || undefined,
    });
    handleClose();
  };

  const handleClose = () => {
    setStep(1);
    setSelectedType(null);
    setName("");
    setHost("");
    setNamespace("");
    setTestResult(null);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="Yeni Kaynak Ekle"
      description={`Adım ${step}/3`}
      size="lg"
    >
      <AnimatePresence mode="wait">
        {/* Step 1: Type Selection */}
        {step === 1 && (
          <motion.div
            key="step1"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="grid grid-cols-3 gap-3"
          >
            {SOURCE_TYPES.map((item) => (
              <button
                key={item.type}
                onClick={() => handleTypeSelect(item.type)}
                className={cn(
                  "flex flex-col items-center gap-3 p-6 rounded-xl border-2 transition-all duration-200",
                  "hover:border-accent/40 hover:bg-accent/5",
                  selectedType === item.type
                    ? "border-accent bg-accent/10"
                    : "border-border-default"
                )}
              >
                <div className="text-accent">{item.icon}</div>
                <span className="text-sm font-medium text-text-primary">
                  {item.label}
                </span>
                <span className="text-xs text-text-muted text-center">
                  {item.description}
                </span>
              </button>
            ))}
          </motion.div>
        )}

        {/* Step 2: Connection Details */}
        {step === 2 && (
          <motion.div
            key="step2"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="space-y-4"
          >
            <Input
              label="Kaynak Adı"
              placeholder="prod-k8s-cluster"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />

            {selectedType === LogSourceType.KUBERNETES && (
              <Input
                label="Namespace"
                placeholder="default"
                value={namespace}
                onChange={(e) => setNamespace(e.target.value)}
              />
            )}

            {selectedType === LogSourceType.SSH && (
              <>
                <Input
                  label="Host"
                  placeholder="192.168.1.100:22"
                  value={host}
                  onChange={(e) => setHost(e.target.value)}
                />
                <Input label="Username" placeholder="root" />
                <Input label="Log File Path" placeholder="/var/log/syslog" />
              </>
            )}

            {selectedType === LogSourceType.DOCKER && (
              <Input
                label="Docker Host"
                placeholder="unix:///var/run/docker.sock"
                value={host}
                onChange={(e) => setHost(e.target.value)}
              />
            )}

            <div className="flex gap-2 pt-2">
              <Button
                variant="secondary"
                icon={<ArrowLeft size={14} />}
                onClick={() => setStep(1)}
              >
                Geri
              </Button>
              <Button
                variant="primary"
                icon={<ArrowRight size={14} />}
                iconPosition="right"
                onClick={() => setStep(3)}
                disabled={!name}
                fullWidth
              >
                Test & Kaydet
              </Button>
            </div>
          </motion.div>
        )}

        {/* Step 3: Test & Save */}
        {step === 3 && (
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
              {testResult === null && !testConnection.isPending && (
                <Button variant="primary" onClick={handleTest}>
                  Bağlantıyı Test Et
                </Button>
              )}

              {testConnection.isPending && (
                <div className="flex items-center gap-2 text-text-secondary">
                  <div className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
                  <span className="text-sm">Bağlantı test ediliyor...</span>
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
                onClick={() => {
                  setStep(2);
                  setTestResult(null);
                }}
              >
                Geri
              </Button>
              <Button
                variant="success"
                fullWidth
                onClick={handleSave}
                loading={createSource.isPending}
                disabled={testResult !== "success"}
              >
                Kaydet
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </Modal>
  );
}

export { AddSourceModal };
