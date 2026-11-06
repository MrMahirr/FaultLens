"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useAuthHydrated } from "@/features/auth/hooks/useAuthHydrated";
import { AuthShell } from "@/shared/components/layout/AuthShell";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token);
  const mustChangePassword = useAuthStore((state) => state.user?.mustChangePassword ?? false);
  const router = useRouter();
  const hydrated = useAuthHydrated();

  useEffect(() => {
    if (token) {
      router.replace(mustChangePassword ? "/change-password" : "/dashboard");
    }
  }, [token, mustChangePassword, router]);

  if (!hydrated) return null;

  // If already authenticated, show empty loader during transition
  if (token) {
    return (
      <div className="min-h-screen bg-bg-primary flex items-center justify-center" role="status">
        <span className="spinner w-6 h-6 text-text-muted" aria-hidden="true" />
      </div>
    );
  }

  return <AuthShell>{children}</AuthShell>;
}
