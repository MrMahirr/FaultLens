"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { AlertTriangle, LogIn } from "lucide-react";
import { Input } from "@/shared/components/ui/Input";
import { PasswordInput } from "@/shared/components/ui/PasswordInput";
import { Button } from "@/shared/components/ui/Button";
import { useValidationMessage } from "@/shared/hooks/useValidationMessage";
import { useLoginMutation } from "@/features/auth/api/useAuthMutation";

/* ── Validation Schema (messages are "validation" keys) ────── */

const loginSchema = z.object({
  username: z.string().min(1, "usernameRequired"),
  password: z.string().min(6, "passwordMin6"),
});

type LoginFormData = z.infer<typeof loginSchema>;

/** Client-side pause after repeated failures; the API has its own rate limit. */
const MAX_ATTEMPTS = 3;
const LOCK_SECONDS = 30;

/* ── Component ─────────────────────────────────────────────── */

function LoginForm() {
  const t = useTranslations("auth.login");
  const v = useValidationMessage();
  const router = useRouter();
  const loginMutation = useLoginMutation();
  const [failures, setFailures] = useState(0);
  const [lockedUntil, setLockedUntil] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());

  const secondsLeft = lockedUntil ? Math.max(0, Math.ceil((lockedUntil - now) / 1000)) : 0;
  const locked = secondsLeft > 0;

  useEffect(() => {
    if (!lockedUntil) return;
    const id = setInterval(() => {
      const current = Date.now();
      setNow(current);
      if (current >= lockedUntil) {
        setLockedUntil(null);
        setFailures(0);
      }
    }, 1000);
    return () => clearInterval(id);
  }, [lockedUntil]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  const onSubmit = (data: LoginFormData) => {
    if (locked) return;
    loginMutation.mutate(data, {
      onSuccess: (response) => {
        router.push(response.mustChangePassword ? "/change-password" : "/dashboard");
      },
      onError: () => {
        const next = failures + 1;
        setFailures(next);
        if (next >= MAX_ATTEMPTS) {
          const start = Date.now();
          setNow(start);
          setLockedUntil(start + LOCK_SECONDS * 1000);
        }
      },
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="stack gap-4" noValidate>
      <Input
        label={t("username")}
        autoComplete="username"
        autoCapitalize="none"
        spellCheck={false}
        autoFocus
        error={v(errors.username?.message)}
        {...register("username")}
      />

      <PasswordInput
        label={t("password")}
        autoComplete="current-password"
        error={v(errors.password?.message)}
        {...register("password")}
      />

      {(loginMutation.isError || locked) && (
        <div className="banner c-crit" role="alert">
          <AlertTriangle size={14} aria-hidden="true" />
          <p>
            {locked
              ? t("locked", { seconds: secondsLeft })
              : loginMutation.error instanceof Error && loginMutation.error.message
                ? loginMutation.error.message
                : t("failed")}
          </p>
        </div>
      )}

      <Button
        type="submit"
        variant="primary"
        size="lg"
        fullWidth
        loading={loginMutation.isPending}
        disabled={locked}
        icon={<LogIn size={16} aria-hidden="true" />}
      >
        {t("submit")}
      </Button>

      <p className="xs t3 text-center">{t("forgot")}</p>
    </form>
  );
}

export { LoginForm };
