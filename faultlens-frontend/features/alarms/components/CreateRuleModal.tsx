"use client";

import { useState } from "react";
import { Modal } from "@/shared/components/ui/Modal";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { Severity } from "@/shared/types/common.types";
import { useAlarmStore } from "../store/alarm.store";
import { BellPlus } from "lucide-react";

interface CreateRuleModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateRuleModal({ isOpen, onClose }: CreateRuleModalProps) {
  const { addRule } = useAlarmStore();

  const [name, setName] = useState("");
  const [condition, setCondition] = useState("");
  const [severity, setSeverity] = useState<Severity>(Severity.WARN);

  const handleSave = () => {
    if (!name.trim() || !condition.trim()) return;

    addRule({
      name,
      condition,
      severity,
      isActive: true,
    });

    setName("");
    setCondition("");
    setSeverity(Severity.WARN);
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Yeni Alarm Kuralı Oluştur">
      <div className="space-y-4 py-4">
        <Input
          label="Kural Adı"
          placeholder="Örn: Yüksek Hata Oranı"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
        />

        <Input
          label="Koşul (Condition)"
          placeholder="Örn: ERROR > 100 in 5m"
          value={condition}
          onChange={(e) => setCondition(e.target.value)}
          hint="Hangi durumda alarm tetiklenecek? (Şu an test için manuel giriş yapınız)"
        />

        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-text-secondary">
            Tetiklenecek Alarm Seviyesi
          </label>
          <select
            className="w-full bg-bg-tertiary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-border-active focus:ring-1 focus:ring-accent/30 cursor-pointer"
            value={severity}
            onChange={(e) => setSeverity(e.target.value as Severity)}
          >
            {Object.values(Severity).map((sev) => (
              <option key={sev} value={sev}>
                {sev}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex justify-end gap-3 mt-6">
        <Button variant="ghost" onClick={onClose}>
          İptal
        </Button>
        <Button
          variant="primary"
          onClick={handleSave}
          disabled={!name.trim() || !condition.trim()}
          icon={<BellPlus size={16} />}
        >
          Kuralı Ekle
        </Button>
      </div>
    </Modal>
  );
}
