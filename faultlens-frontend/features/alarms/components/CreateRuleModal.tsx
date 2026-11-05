"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { BellPlus } from "lucide-react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { Modal } from "@/shared/components/ui/Modal";
import { Severity } from "@/shared/types/common.types";
import { useCreateAlarmRuleMutation } from "../api/useAlarmRules";
import {
  DEFAULT_RULE_FORM,
  describeRule,
  ruleFormSchema,
  toRuleInput,
  type RuleFormValues,
} from "../lib/ruleForm";
import { ALARM_CHANNEL_LABELS, AlarmChannel, AlarmRuleType } from "../types/alarm.types";

interface CreateRuleModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const TYPE_LABELS: Record<AlarmRuleType, string> = {
  [AlarmRuleType.THRESHOLD]: "Eşik — belirli sürede N adet log",
  [AlarmRuleType.PATTERN]: "Desen — mesaj düzenli ifadeyle eşleşirse",
  [AlarmRuleType.NEW_GROUP]: "Yeni hata — daha önce görülmemiş hata grubu",
};

const selectClass =
  "w-full bg-bg-tertiary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-border-active focus:ring-1 focus:ring-accent/30 cursor-pointer";

export function CreateRuleModal({ isOpen, onClose }: CreateRuleModalProps) {
  const createRule = useCreateAlarmRuleMutation();
  const { data: sources } = useSourcesQuery();
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<RuleFormValues>({
    resolver: zodResolver(ruleFormSchema),
    defaultValues: DEFAULT_RULE_FORM,
  });
  const values = useWatch({ control }) as RuleFormValues;
  const type = values.type ?? DEFAULT_RULE_FORM.type;

  const close = () => {
    reset(DEFAULT_RULE_FORM);
    onClose();
  };

  const onSubmit = (form: RuleFormValues) => {
    createRule.mutate(toRuleInput(form), { onSuccess: close });
  };

  return (
    <Modal isOpen={isOpen} onClose={close} title="Yeni Alarm Kuralı Oluştur">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-4" noValidate>
        <Input
          label="Kural Adı"
          placeholder="Örn: Yüksek Hata Oranı"
          error={errors.name?.message}
          autoFocus
          {...register("name")}
        />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="rule-type" className="text-sm font-medium text-text-secondary">
            Kural Tipi
          </label>
          <select id="rule-type" className={selectClass} {...register("type")}>
            {Object.values(AlarmRuleType).map((value) => (
              <option key={value} value={value}>
                {TYPE_LABELS[value]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="rule-severity" className="text-sm font-medium text-text-secondary">
            En Düşük Seviye
          </label>
          <select id="rule-severity" className={selectClass} {...register("severity")}>
            {Object.values(Severity).map((value) => (
              <option key={value} value={value}>
                {value} ve üzeri
              </option>
            ))}
          </select>
        </div>

        {type === AlarmRuleType.THRESHOLD && (
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Pencere (dakika)"
              type="number"
              min={1}
              max={60}
              error={errors.windowMinutes?.message}
              {...register("windowMinutes")}
            />
            <Input
              label="Eşik (adet)"
              type="number"
              min={1}
              error={errors.threshold?.message}
              {...register("threshold")}
            />
          </div>
        )}

        {type === AlarmRuleType.PATTERN && (
          <Input
            label="Desen (düzenli ifade)"
            placeholder="Örn: Connection (refused|reset)"
            error={errors.pattern?.message}
            hint="Mesaj veya stack trace içinde aranır"
            {...register("pattern")}
          />
        )}

        <fieldset className="space-y-1.5">
          <legend className="text-sm font-medium text-text-secondary">
            Kaynaklar (seçilmezse tümü)
          </legend>
          <Controller
            control={control}
            name="sourceIds"
            render={({ field }) => (
              <div className="flex flex-wrap gap-3">
                {(sources ?? []).map((source) => (
                  <label
                    key={source.id}
                    className="flex items-center gap-2 text-sm text-text-primary"
                  >
                    <input
                      type="checkbox"
                      checked={field.value.includes(source.id)}
                      onChange={(e) =>
                        field.onChange(
                          e.target.checked
                            ? [...field.value, source.id]
                            : field.value.filter((id) => id !== source.id),
                        )
                      }
                    />
                    {source.name}
                  </label>
                ))}
                {(sources ?? []).length === 0 && (
                  <span className="text-xs text-text-muted">Kayıtlı kaynak yok</span>
                )}
              </div>
            )}
          />
        </fieldset>

        <fieldset className="space-y-1.5">
          <legend className="text-sm font-medium text-text-secondary">Bildirim Kanalları</legend>
          <Controller
            control={control}
            name="channels"
            render={({ field }) => (
              <div className="flex flex-wrap gap-3">
                {Object.values(AlarmChannel).map((channel) => (
                  <label
                    key={channel}
                    className="flex items-center gap-2 text-sm text-text-primary"
                  >
                    <input
                      type="checkbox"
                      checked={field.value.includes(channel)}
                      onChange={(e) =>
                        field.onChange(
                          e.target.checked
                            ? [...field.value, channel]
                            : field.value.filter((c) => c !== channel),
                        )
                      }
                    />
                    {ALARM_CHANNEL_LABELS[channel]}
                  </label>
                ))}
              </div>
            )}
          />
          {errors.channels?.message && (
            <p className="text-xs text-error">{errors.channels.message}</p>
          )}
        </fieldset>

        <Input
          label="Bekleme süresi (dakika)"
          type="number"
          min={0}
          max={1440}
          error={errors.cooldownMinutes?.message}
          hint="Aynı kural bu süre içinde tekrar alarm üretmez"
          {...register("cooldownMinutes")}
        />

        <p className="text-xs text-text-muted" data-testid="rule-preview">
          Koşul:{" "}
          {describeRule({
            type,
            severity: values.severity ?? DEFAULT_RULE_FORM.severity,
            windowSec: Number(values.windowMinutes ?? 0) * 60,
            threshold: Number(values.threshold ?? 0),
            pattern: values.pattern ?? "",
          })}
        </p>

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={close}>
            İptal
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={createRule.isPending}
            icon={<BellPlus size={16} />}
          >
            Kuralı Ekle
          </Button>
        </div>
      </form>
    </Modal>
  );
}
