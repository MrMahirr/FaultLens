"use client";

import { useTranslations } from "next-intl";
import { Server, Terminal, Container, FileText } from "lucide-react";
import { LogSourceType } from "@/shared/types/common.types";
import { cn } from "@/shared/lib/utils";

const SOURCE_TYPES = [
  { type: LogSourceType.KUBERNETES, icon: <Server size={20} /> },
  { type: LogSourceType.SSH, icon: <Terminal size={20} /> },
  { type: LogSourceType.DOCKER, icon: <Container size={20} /> },
  { type: LogSourceType.LOCAL_FILE, icon: <FileText size={20} /> },
] as const;

interface SourceTypeSelectionStepProps {
  selectedType: LogSourceType | null;
  onSelect: (type: LogSourceType) => void;
}

export function SourceTypeSelectionStep({ selectedType, onSelect }: SourceTypeSelectionStepProps) {
  const t = useTranslations("sources");
  return (
    <fieldset className="swA">
      <legend className="label mb-2">{t("wizard.chooseType")}</legend>
      <div className="grid grid-cols-2 gap-3">
        {SOURCE_TYPES.map((item) => {
          const selected = selectedType === item.type;
          return (
            <button
              key={item.type}
              type="button"
              aria-pressed={selected}
              onClick={() => onSelect(item.type)}
              className={cn(
                "card hov p-4 flex flex-col items-start gap-2 text-left",
                selected && "border-border-hover bg-bg-glass-hover",
              )}
            >
              <span className="t2 inline-flex" aria-hidden="true">
                {item.icon}
              </span>
              <span className="small font-medium">{t(`types.${item.type}`)}</span>
              <span className="xs t3">{t(`typeDescriptions.${item.type}`)}</span>
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}
