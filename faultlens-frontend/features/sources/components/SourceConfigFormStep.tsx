import { motion } from "framer-motion";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { LogSourceType } from "@/shared/types/common.types";

interface SourceConfigFormStepProps {
  selectedType: LogSourceType | null;
  name: string;
  setName: (v: string) => void;
  host: string;
  setHost: (v: string) => void;
  namespace: string;
  setNamespace: (v: string) => void;
  username: string;
  setUsername: (v: string) => void;
  password: string;
  setPassword: (v: string) => void;
  logFilePath: string;
  setLogFilePath: (v: string) => void;
  onBack: () => void;
  onNext: () => void;
}

export function SourceConfigFormStep({
  selectedType,
  name, setName,
  host, setHost,
  namespace, setNamespace,
  username, setUsername,
  password, setPassword,
  logFilePath, setLogFilePath,
  onBack,
  onNext
}: SourceConfigFormStepProps) {
  return (
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
            label="Host (IP:Port)"
            placeholder="127.0.0.1:22"
            value={host}
            onChange={(e) => setHost(e.target.value)}
          />
          <Input 
            label="Username" 
            placeholder="root" 
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <Input 
            label="Password" 
            type="password" 
            placeholder="***" 
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Input 
            label="Log File Path" 
            placeholder="/var/log/syslog" 
            value={logFilePath}
            onChange={(e) => setLogFilePath(e.target.value)}
          />
        </>
      )}

      {selectedType === LogSourceType.DOCKER && (
        <>
          <Input
            label="Container ID veya İsmi"
            placeholder="faultlens-frontend"
            value={logFilePath}
            onChange={(e) => setLogFilePath(e.target.value)}
          />
          <Input
            label="Docker Host (Opsiyonel)"
            placeholder="Boş bırakırsan lokal bilgisayarı kullanır"
            value={host}
            onChange={(e) => setHost(e.target.value)}
          />
        </>
      )}

      {selectedType === LogSourceType.LOCAL_FILE && (
        <Input
          label="Log Dosyası Yolu (Absolute Path)"
          placeholder="C:\logs\app.log veya /var/log/syslog"
          value={logFilePath}
          onChange={(e) => setLogFilePath(e.target.value)}
        />
      )}

      <div className="flex gap-2 pt-2">
        <Button
          variant="secondary"
          icon={<ArrowLeft size={14} />}
          onClick={onBack}
        >
          Geri
        </Button>
        <Button
          variant="primary"
          icon={<ArrowRight size={14} />}
          iconPosition="right"
          onClick={onNext}
          disabled={!name}
          fullWidth
        >
          Test & Kaydet
        </Button>
      </div>
    </motion.div>
  );
}
