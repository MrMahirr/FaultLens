import { describe, expect, it } from "vitest";
import { Severity } from "@/shared/types/common.types";
import { AlarmChannel, AlarmRuleType } from "../types/alarm.types";
import { DEFAULT_RULE_FORM, describeRule, ruleFormSchema, toRuleInput } from "./ruleForm";

describe("ruleFormSchema", () => {
  it("accepts the defaults (a threshold rule)", () => {
    expect(ruleFormSchema.safeParse({ ...DEFAULT_RULE_FORM, name: "Hata patlaması" }).success).toBe(
      true,
    );
  });

  it("requires a valid pattern for PATTERN rules only", () => {
    const base = { ...DEFAULT_RULE_FORM, name: "x", type: AlarmRuleType.PATTERN };

    expect(ruleFormSchema.safeParse({ ...base, pattern: "" }).error?.issues[0].path).toEqual([
      "pattern",
    ]);
    expect(
      ruleFormSchema.safeParse({ ...base, pattern: "(unclosed" }).error?.issues[0].message,
    ).toBe("Geçersiz düzenli ifade");
    expect(
      ruleFormSchema.safeParse({ ...base, pattern: "Connection (refused|reset)" }).success,
    ).toBe(true);
    expect(
      ruleFormSchema.safeParse({ ...DEFAULT_RULE_FORM, name: "x", type: AlarmRuleType.NEW_GROUP })
        .success,
    ).toBe(true);
  });

  it("mirrors the backend limits", () => {
    const issues = (overrides: object) =>
      ruleFormSchema.safeParse({ ...DEFAULT_RULE_FORM, name: "x", ...overrides }).error?.issues ??
      [];

    expect(issues({ windowMinutes: 61 })[0].path).toEqual(["windowMinutes"]);
    expect(issues({ threshold: 0 })[0].path).toEqual(["threshold"]);
    expect(issues({ cooldownMinutes: 1441 })[0].path).toEqual(["cooldownMinutes"]);
    expect(issues({ channels: [] })[0].message).toBe("En az bir kanal seçin");
    expect(issues({ name: "   " })[0].message).toBe("Kural adı gereklidir");
  });
});

describe("toRuleInput", () => {
  it("converts minutes to seconds and sends only the chosen type's fields", () => {
    expect(
      toRuleInput({
        ...DEFAULT_RULE_FORM,
        name: "  Hata patlaması ",
        windowMinutes: 5,
        threshold: 50,
        pattern: "leftover",
        cooldownMinutes: 15,
      }),
    ).toEqual({
      name: "Hata patlaması",
      type: AlarmRuleType.THRESHOLD,
      severity: Severity.ERROR,
      windowSec: 300,
      threshold: 50,
      sourceIds: [],
      channels: [AlarmChannel.IN_APP],
      cooldownSec: 900,
    });

    const pattern = toRuleInput({
      ...DEFAULT_RULE_FORM,
      name: "p",
      type: AlarmRuleType.PATTERN,
      pattern: "refused",
    });
    expect(pattern).not.toHaveProperty("windowSec");
    expect(pattern.pattern).toBe("refused");
  });
});

describe("describeRule", () => {
  it("summarises each rule type", () => {
    expect(
      describeRule({
        type: AlarmRuleType.THRESHOLD,
        severity: Severity.ERROR,
        windowSec: 300,
        threshold: 50,
        pattern: null,
      }),
    ).toBe("5 dk içinde ≥ 50 adet ERROR+ log");
    expect(
      describeRule({
        type: AlarmRuleType.PATTERN,
        severity: Severity.WARN,
        windowSec: null,
        threshold: null,
        pattern: "refused",
      }),
    ).toBe("WARN+ log /refused/ ile eşleşirse");
    expect(
      describeRule({
        type: AlarmRuleType.NEW_GROUP,
        severity: Severity.CRITICAL,
        windowSec: null,
        threshold: null,
        pattern: null,
      }),
    ).toBe("Yeni CRITICAL+ hata grubu oluşursa");
  });
});
