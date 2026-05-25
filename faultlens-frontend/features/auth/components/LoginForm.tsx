"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { LogIn } from "lucide-react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { useLogin } from "@/features/auth/api/auth.queries";
import { motion, AnimatePresence } from "framer-motion";

/* ── Validation Schema ─────────────────────────────────────── */

const loginSchema = z.object({
  username: z.string().min(1, "Kullanıcı adı gereklidir"),
  password: z.string().min(6, "Şifre en az 6 karakter olmalıdır"),
});

type LoginFormData = z.infer<typeof loginSchema>;

/* ── Component ─────────────────────────────────────────────── */

function LoginForm() {
  const router = useRouter();
  const loginMutation = useLogin();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const onSubmit = (data: LoginFormData) => {
    loginMutation.mutate(data, {
      onSuccess: () => {
        router.push("/dashboard");
      },
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      <Input
        label="Kullanıcı Adı"
        placeholder="admin"
        error={errors.username?.message}
        {...register("username")}
      />

      <Input
        label="Şifre"
        type="password"
        placeholder="••••••"
        error={errors.password?.message}
        {...register("password")}
      />

      {/* Error Message */}
      <AnimatePresence mode="wait">
        {loginMutation.isError && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="flex items-center gap-2 p-3 rounded-lg bg-error/10 border border-error/20"
          >
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-error shrink-0" />
            <p className="text-sm text-error">
              {loginMutation.error instanceof Error
                ? loginMutation.error.message
                : "Giriş yapılamadı. Lütfen tekrar deneyin."}
            </p>
          </motion.div>
        )}
      </AnimatePresence>

      <Button
        type="submit"
        variant="primary"
        size="lg"
        fullWidth
        loading={loginMutation.isPending}
        icon={<LogIn size={18} />}
      >
        Giriş Yap
      </Button>

      {/* Dev hint */}
      <p className="text-xs text-text-muted text-center">
        Demo: <span className="font-mono text-text-secondary">admin</span> /{" "}
        <span className="font-mono text-text-secondary">admin123</span>
      </p>
    </form>
  );
}

export { LoginForm };
