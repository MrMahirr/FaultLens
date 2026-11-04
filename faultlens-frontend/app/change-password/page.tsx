"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ShieldAlert } from "lucide-react";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { ChangePasswordForm } from "@/features/auth/components/ChangePasswordForm";
import { useAuthHydrated } from "@/features/auth/hooks/useAuthHydrated";

/**
 * Standalone (no sidebar) page. Users created by bootstrap, or whose password
 * was reset from a well-known default, land here after login; the backend
 * rejects other API calls until the password is changed.
 */
export default function ChangePasswordPage() {
  const router = useRouter();
  const token = useAuthStore((state) => state.token);
  const mustChangePassword = useAuthStore((state) => state.user?.mustChangePassword ?? false);
  const logout = useAuthStore((state) => state.logout);
  const hydrated = useAuthHydrated();

  useEffect(() => {
    if (hydrated && !token) router.replace("/login");
  }, [hydrated, token, router]);

  if (!hydrated || !token) return null;

  const handleLogout = () => {
    logout();
    router.replace("/login");
  };

  return (
    <div className="min-h-screen bg-bg-primary bg-grid-pattern bg-accent-glow flex items-center justify-center p-4">
      <div className="relative z-10 w-full max-w-md glass rounded-2xl p-8 space-y-6">
        <div className="text-center space-y-3">
          <div className="flex items-center justify-center">
            <div className="w-10 h-10 rounded-xl bg-accent/10 flex items-center justify-center">
              <ShieldAlert size={22} className="text-accent" />
            </div>
          </div>
          <div>
            <h1 className="text-2xl font-bold font-display text-text-primary">
              Şifrenizi Değiştirin
            </h1>
            <p className="text-sm text-text-secondary mt-1">
              {mustChangePassword
                ? "Güvenliğiniz için devam etmeden önce yeni bir şifre belirlemeniz gerekiyor."
                : "Hesabınız için yeni bir şifre belirleyin."}
            </p>
          </div>
        </div>

        <ChangePasswordForm onSuccess={() => router.replace("/dashboard")} />

        <button
          type="button"
          onClick={handleLogout}
          className="w-full text-xs text-text-muted hover:text-text-secondary transition-colors"
        >
          Çıkış yap
        </button>
      </div>
    </div>
  );
}
