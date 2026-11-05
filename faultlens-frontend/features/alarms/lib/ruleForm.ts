import { z } from "zod";
import { Severity } from "@/shared/types/common.types";
import {
  AlarmChannel,
  AlarmRuleType,
  type AlarmRule,
  type AlarmRuleInput,
} from "../types/alarm.types";

/**
 * Form schema for an alarm rule. Mirrors the backend AlarmRuleRequest limits (window 1–60 min,
 * threshold ≥ 1, pattern ≤ 500 chars, cooldown 0–1440 min) and its per-type requirements, so
 * the user sees the error before the request.
 */
export const ruleFormSchema = z
  .object({
    name: z.string().trim().min(1, "Kural adı gereklidir").max(200, "En fazla 200 karakter"),
    type: z.enum(AlarmRuleType),
    severity: z.enum(Severity),
    windowMinutes: z.coerce
      .number<number>()
      .int()
      .min(1, "En az 1 dakika")
      .max(60, "En fazla 60 dakika"),
    threshold: z.coerce.number<number>().int().min(1, "En az 1").max(1_000_000),
    pattern: z.string().max(500, "En fazla 500 karakter"),
    sourceIds: z.array(z.number()),
    channels: z.array(z.enum(AlarmChannel)).min(1, "En az bir kanal seçin"),
    cooldownMinutes: z.coerce.number<number>().int().min(0).max(1440, "En fazla 1440 dakika"),
  })
  .superRefine((value, ctx) => {
    if (value.type === AlarmRuleType.PATTERN) {
      if (!value.pattern.trim()) {
        ctx.addIssue({ code: "custom", path: ["pattern"], message: "Desen gereklidir" });
        return;
      }
      try {
        new RegExp(value.pattern);
      } catch {
        ctx.addIssue({ code: "custom", path: ["pattern"], message: "Geçersiz düzenli ifade" });
      }
    }
  });

export type RuleFormValues = z.infer<typeof ruleFormSchema>;

export const DEFAULT_RULE_FORM: RuleFormValues = {
  name: "",
  type: AlarmRuleType.THRESHOLD,
  severity: Severity.ERROR,
  windowMinutes: 5,
  threshold: 50,
  pattern: "",
  sourceIds: [],
  channels: [AlarmChannel.IN_APP],
  cooldownMinutes: 15,
};

/** Form values → API payload: only the fields of the chosen type are sent. */
export function toRuleInput(values: RuleFormValues): AlarmRuleInput {
  return {
    name: values.name.trim(),
    type: values.type,
    severity: values.severity,
    ...(values.type === AlarmRuleType.THRESHOLD
      ? { windowSec: values.windowMinutes * 60, threshold: values.threshold }
      : {}),
    ...(values.type === AlarmRuleType.PATTERN ? { pattern: values.pattern } : {}),
    sourceIds: values.sourceIds,
    channels: values.channels,
    cooldownSec: values.cooldownMinutes * 60,
  };
}

/** One-line, human-readable condition of a rule. */
export function describeRule(
  rule: Pick<AlarmRule, "type" | "severity" | "windowSec" | "threshold" | "pattern">,
) {
  switch (rule.type) {
    case AlarmRuleType.THRESHOLD:
      return `${Math.round((rule.windowSec ?? 0) / 60)} dk içinde ≥ ${rule.threshold} adet ${rule.severity}+ log`;
    case AlarmRuleType.PATTERN:
      return `${rule.severity}+ log /${rule.pattern ?? ""}/ ile eşleşirse`;
    case AlarmRuleType.NEW_GROUP:
      return `Yeni ${rule.severity}+ hata grubu oluşursa`;
  }
}
