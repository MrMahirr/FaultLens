"use client";

import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslations } from "next-intl";
import { AlertTriangle, KeyRound } from "lucide-react";
import { PasswordInput } from "@/shared/components/ui/PasswordInput";
import { Button } from "@/shared/components/ui/Button";
import { useValidationMessage } from "@/shared/hooks/useValidationMessage";
import { getApiErrorCode, getApiErrorMessage } from "@/shared/api/errors";
import { useChangePasswordMutation } from "@/features/auth/api/useAuthMutation";

/* ── Validation Schema (messages are "validation" keys) ────── */

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "currentPasswordRequired"),
    newPassword: z.string().min(8, "newPasswordMin").max(128, "max128"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "passwordMismatch",
    path: ["confirmPassword"],
  })
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: "passwordSame",
    path: ["newPassword"],
  });

type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

/* ── Strength ──────────────────────────────────────────────── */

type Rule = "length" | "case" | "digit" | "symbol" | "different";

function checkRules(password: string, current: string): Record<Rule, boolean> {
  return {
    length: password.length >= 8,
    case: /[a-z]/.test(password) && /[A-Z]/.test(password),
    digit: /\d/.test(password),
    symbol: /[^A-Za-z0-9]/.test(password),
    different: password.length > 0 && password !== current,
  };
}

/** 0–4: one point per guideline met beyond the minimum length, 0 if too short. */
function strengthOf(rules: Record<Rule, boolean>, password: string): 0 | 1 | 2 | 3 | 4 {
  if (!rules.length) return 0;
  const score = [rules.case, rules.digit, rules.symbol, password.length >= 12].filter(
    Boolean,
  ).length;
  return Math.min(4, Math.max(1, score)) as 1 | 2 | 3 | 4;
}

const STRENGTH_COLOR = ["var(--crit)", "var(--crit)", "var(--warn)", "var(--ok)", "var(--ok)"];

/* ── Component ─────────────────────────────────────────────── */

interface ChangePasswordFormProps {
  onSuccess: () => void;
}

function ChangePasswordForm({ onSuccess }: ChangePasswordFormProps) {
  const t = useTranslations("auth.changePassword");
  const v = useValidationMessage();
  const mutation = useChangePasswordMutation();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const [current = "", next = ""] = useWatch({ control, name: ["currentPassword", "newPassword"] });
  const rules = checkRules(next, current);
  const strength = strengthOf(rules, next);

  const onSubmit = ({ currentPassword, newPassword }: ChangePasswordFormData) => {
    mutation.mutate({ currentPassword, newPassword }, { onSuccess });
  };

  const submitError = (() => {
    const code = getApiErrorCode(mutation.error);
    if (code === "INVALID_CURRENT_PASSWORD") return t("errors.invalidCurrent");
    if (code === "PASSWORD_REUSED") return v("passwordSame");
    return getApiErrorMessage(mutation.error, t("errors.failed"));
  })();

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="stack gap-4" noValidate>
      <PasswordInput
        label={t("current")}
        autoComplete="current-password"
        error={v(errors.currentPassword?.message)}
        {...register("currentPassword")}
      />

      <div className="stack gap-2">
        <PasswordInput
          label={t("new")}
          autoComplete="new-password"
          error={v(errors.newPassword?.message)}
          {...register("newPassword")}
        />
        {next && (
          <>
            <div
              className="strength"
              role="meter"
              aria-valuemin={0}
              aria-valuemax={4}
              aria-valuenow={strength}
              aria-label={t("strength", { level: t(`levels.${strength}`) })}
            >
              {[1, 2, 3, 4].map((i) => (
                <i
                  key={i}
                  className={i <= strength ? "on" : undefined}
                  style={
                    i <= strength ? { ["--c" as string]: STRENGTH_COLOR[strength] } : undefined
                  }
                />
              ))}
            </div>
            <p className="xs t3" aria-live="polite">
              {t("strength", { level: t(`levels.${strength}`) })}
            </p>
          </>
        )}
        <ul className="checklist" aria-label={t("new")}>
          {(Object.keys(rules) as Rule[]).map((rule) => (
            <li key={rule} className={rules[rule] ? "ok" : undefined}>
              {t(`rules.${rule}`)}
            </li>
          ))}
        </ul>
      </div>

      <PasswordInput
        label={t("confirm")}
        autoComplete="new-password"
        error={v(errors.confirmPassword?.message)}
        {...register("confirmPassword")}
      />

      {mutation.isError && (
        <div className="banner c-crit" role="alert">
          <AlertTriangle size={14} aria-hidden="true" />
          <p>{submitError}</p>
        </div>
      )}

      <Button
        type="submit"
        variant="primary"
        size="lg"
        fullWidth
        loading={mutation.isPending}
        icon={<KeyRound size={16} aria-hidden="true" />}
      >
        {t("submit")}
      </Button>
    </form>
  );
}

export { ChangePasswordForm };
