"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { KeyRound } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { useChangePasswordMutation } from "@/features/auth/api/useAuthMutation";

/* ── Validation Schema ─────────────────────────────────────── */

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Mevcut şifre gereklidir"),
    newPassword: z
      .string()
      .min(8, "Yeni şifre en az 8 karakter olmalıdır")
      .max(128, "Yeni şifre en fazla 128 karakter olabilir"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Şifreler eşleşmiyor",
    path: ["confirmPassword"],
  })
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: "Yeni şifre mevcut şifreden farklı olmalıdır",
    path: ["newPassword"],
  });

type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;

/* ── Component ─────────────────────────────────────────────── */

interface ChangePasswordFormProps {
  onSuccess: () => void;
}

function errorMessage(error: unknown): string {
  const apiMessage = (
    error as { response?: { data?: { error?: { code?: string; message?: string } } } }
  )?.response?.data?.error;
  if (apiMessage?.code === "INVALID_CURRENT_PASSWORD") return "Mevcut şifre hatalı.";
  if (apiMessage?.code === "PASSWORD_REUSED") return "Yeni şifre mevcut şifreden farklı olmalıdır.";
  return apiMessage?.message ?? "Şifre değiştirilemedi. Lütfen tekrar deneyin.";
}

function ChangePasswordForm({ onSuccess }: ChangePasswordFormProps) {
  const mutation = useChangePasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const onSubmit = ({ currentPassword, newPassword }: ChangePasswordFormData) => {
    mutation.mutate({ currentPassword, newPassword }, { onSuccess });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <Input
        label="Mevcut Şifre"
        type="password"
        autoComplete="current-password"
        error={errors.currentPassword?.message}
        {...register("currentPassword")}
      />

      <Input
        label="Yeni Şifre"
        type="password"
        autoComplete="new-password"
        error={errors.newPassword?.message}
        {...register("newPassword")}
      />

      <Input
        label="Yeni Şifre (Tekrar)"
        type="password"
        autoComplete="new-password"
        error={errors.confirmPassword?.message}
        {...register("confirmPassword")}
      />

      <AnimatePresence mode="wait">
        {mutation.isError && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="flex items-center gap-2 p-3 rounded-lg bg-error/10 border border-error/20"
          >
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-error shrink-0" />
            <p className="text-sm text-error">{errorMessage(mutation.error)}</p>
          </motion.div>
        )}
      </AnimatePresence>

      <Button
        type="submit"
        variant="primary"
        size="lg"
        fullWidth
        loading={mutation.isPending}
        icon={<KeyRound size={18} />}
      >
        Şifreyi Değiştir
      </Button>
    </form>
  );
}

export { ChangePasswordForm };
