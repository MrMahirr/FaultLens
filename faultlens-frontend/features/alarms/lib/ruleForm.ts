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
 * the user sees the error before the request. Error messages are keys of the "validation"
 * message namespace; the form translates them.
 */
export const ruleFormSchema = z
  .object({
    name: z.string().trim().min(1, "ruleNameRequired").max(200, "max200"),
    type: z.enum(AlarmRuleType),
    severity: z.enum(Severity),
    windowMinutes: z.coerce
      .number<number>()
      .int()
      .min(1, "minOneMinute")
      .max(60, "maxSixtyMinutes"),
    threshold: z.coerce.number<number>().int().min(1, "minOne").max(1_000_000),
    pattern: z.string().max(500, "max500"),
    sourceIds: z.array(z.number()),
    channels: z.array(z.enum(AlarmChannel)).min(1, "channelRequired"),
    cooldownMinutes: z.coerce.number<number>().int().min(0).max(1440, "maxDay"),
  })
  .superRefine((value, ctx) => {
    if (value.type === AlarmRuleType.PATTERN) {
      if (!value.pattern.trim()) {
        ctx.addIssue({ code: "custom", path: ["pattern"], message: "patternRequired" });
        return;
      }
      try {
        new RegExp(value.pattern);
      } catch {
        ctx.addIssue({ code: "custom", path: ["pattern"], message: "patternInvalid" });
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

/** Translator of the "alarms.rules.describe" namespace. */
export type DescribeRuleTranslator = (
  key: AlarmRuleType,
  values: Record<string, string | number>,
) => string;

/** One-line, human-readable condition of a rule. */
export function describeRule(
  rule: Pick<AlarmRule, "type" | "severity" | "windowSec" | "threshold" | "pattern">,
  t: DescribeRuleTranslator,
) {
  return t(rule.type, {
    minutes: Math.round((rule.windowSec ?? 0) / 60),
    threshold: rule.threshold ?? 0,
    severity: rule.severity,
    pattern: rule.pattern ?? "",
  });
}
