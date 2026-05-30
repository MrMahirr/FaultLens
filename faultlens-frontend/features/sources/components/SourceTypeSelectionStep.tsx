import { motion } from "framer-motion";
import { Server, Terminal, Container, FileText } from "lucide-react";
import { LogSourceType } from "@/shared/types/common.types";
import { cn } from "@/shared/lib/utils";

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
  {
    type: LogSourceType.LOCAL_FILE,
    label: "Local File",
    icon: <FileText size={28} />,
    description: "Lokal makinedeki log dosyasını izleyin",
  },
];

interface SourceTypeSelectionStepProps {
  selectedType: LogSourceType | null;
  onSelect: (type: LogSourceType) => void;
}

export function SourceTypeSelectionStep({ selectedType, onSelect }: SourceTypeSelectionStepProps) {
  return (
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
          onClick={() => onSelect(item.type)}
          className={cn(
            "flex flex-col items-center gap-3 p-6 rounded-xl border-2 transition-all duration-200 cursor-pointer",
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
  );
}
