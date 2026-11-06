"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useAuthHydrated } from "@/features/auth/hooks/useAuthHydrated";

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

  return (
    <div className="min-h-screen bg-bg-primary bg-grid-pattern bg-accent-glow flex items-center justify-center p-4">
      <div className="relative z-10 w-full max-w-md">{children}</div>
    </div>
  );
}
