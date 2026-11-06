"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { BellPlus } from "lucide-react";
import { useTranslations } from "next-intl";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useSourcesQuery } from "@/features/sources/api/useSourceQuery";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { Modal } from "@/shared/components/ui/Modal";
import { useValidationMessage } from "@/shared/hooks/useValidationMessage";
import { Severity } from "@/shared/types/common.types";
import { useCreateAlarmRuleMutation } from "../api/useAlarmRules";
import {
  DEFAULT_RULE_FORM,
  describeRule,
  ruleFormSchema,
  toRuleInput,
  type RuleFormValues,
} from "../lib/ruleForm";
import { AlarmChannel, AlarmRuleType } from "../types/alarm.types";

interface CreateRuleModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateRuleModal({ isOpen, onClose }: CreateRuleModalProps) {
  const t = useTranslations("alarms");
  const tDescribe = useTranslations("alarms.rules.describe");
  const tCommon = useTranslations("common");
  const v = useValidationMessage();
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
    <Modal
      isOpen={isOpen}
      onClose={close}
      title={t("form.title")}
      footer={
        <>
          <Button variant="ghost" onClick={close}>
            {tCommon("cancel")}
          </Button>
          <Button
            type="submit"
            form="create-rule-form"
            variant="primary"
            loading={createRule.isPending}
            icon={<BellPlus size={16} aria-hidden="true" />}
          >
            {t("form.submit")}
          </Button>
        </>
      }
    >
      <form
        id="create-rule-form"
        onSubmit={handleSubmit(onSubmit)}
        className="stack gap-4"
        noValidate
      >
        <Input
          label={t("form.name")}
          placeholder={t("form.namePlaceholder")}
          error={v(errors.name?.message)}
          autoFocus
          {...register("name")}
        />

        <div className="field">
          <label htmlFor="rule-type" className="label">
            {t("form.type")}
          </label>
          <select id="rule-type" className="input" {...register("type")}>
            {Object.values(AlarmRuleType).map((value) => (
              <option key={value} value={value}>
                {t(`form.types.${value}`)}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="rule-severity" className="label">
            {t("form.severity")}
          </label>
          <select id="rule-severity" className="input" {...register("severity")}>
            {Object.values(Severity).map((value) => (
              <option key={value} value={value}>
                {t("form.severityOption", { severity: value })}
              </option>
            ))}
          </select>
        </div>

        {type === AlarmRuleType.THRESHOLD && (
          <div className="grid grid-cols-2 gap-3">
            <Input
              label={t("form.window")}
              type="number"
              min={1}
              max={60}
              error={v(errors.windowMinutes?.message)}
              {...register("windowMinutes")}
            />
            <Input
              label={t("form.threshold")}
              type="number"
              min={1}
              error={v(errors.threshold?.message)}
              {...register("threshold")}
            />
          </div>
        )}

        {type === AlarmRuleType.PATTERN && (
          <Input
            label={t("form.pattern")}
            placeholder={t("form.patternPlaceholder")}
            error={v(errors.pattern?.message)}
            hint={t("form.patternHint")}
            className="mono"
            {...register("pattern")}
          />
        )}

        <fieldset className="field">
          <legend className="label mb-1.5">{t("form.sources")}</legend>
          <Controller
            control={control}
            name="sourceIds"
            render={({ field }) => (
              <div className="flex flex-wrap gap-x-4 gap-y-2">
                {(sources ?? []).map((source) => (
                  <label key={source.id} className="flex items-center gap-2 small">
                    <input
                      type="checkbox"
                      className="check"
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
                  <span className="help">{t("form.noSources")}</span>
                )}
              </div>
            )}
          />
        </fieldset>

        <fieldset className="field">
          <legend className="label mb-1.5">{t("form.channels")}</legend>
          <Controller
            control={control}
            name="channels"
            render={({ field }) => (
              <div className="flex flex-wrap gap-x-4 gap-y-2">
                {Object.values(AlarmChannel).map((channel) => (
                  <label key={channel} className="flex items-center gap-2 small">
                    <input
                      type="checkbox"
                      className="check"
                      checked={field.value.includes(channel)}
                      onChange={(e) =>
                        field.onChange(
                          e.target.checked
                            ? [...field.value, channel]
                            : field.value.filter((c) => c !== channel),
                        )
                      }
                    />
                    {t(`channel.${channel}`)}
                  </label>
                ))}
              </div>
            )}
          />
          {errors.channels?.message && (
            <p className="err" role="alert">
              {v(errors.channels.message)}
            </p>
          )}
        </fieldset>

        <Input
          label={t("form.cooldown")}
          type="number"
          min={0}
          max={1440}
          error={v(errors.cooldownMinutes?.message)}
          hint={t("form.cooldownHint")}
          {...register("cooldownMinutes")}
        />

        <p className="help" data-testid="rule-preview" aria-live="polite">
          {t("form.preview")}{" "}
          <span className="mono t2">
            {describeRule(
              {
                type,
                severity: values.severity ?? DEFAULT_RULE_FORM.severity,
                windowSec: Number(values.windowMinutes ?? 0) * 60,
                threshold: Number(values.threshold ?? 0),
                pattern: values.pattern ?? "",
              },
              tDescribe,
            )}
          </span>
        </p>
      </form>
    </Modal>
  );
}
