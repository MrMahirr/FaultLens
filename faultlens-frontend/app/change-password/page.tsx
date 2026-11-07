"use client";

import { revokeSession } from "@/shared/api/session";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { AuthShell } from "@/shared/components/layout/AuthShell";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { ChangePasswordForm } from "@/features/auth/components/ChangePasswordForm";
import { useSession } from "@/features/auth/hooks/useSession";

/**
 * Standalone (no sidebar) page. Users created by bootstrap, or whose password
 * was reset from a well-known default, land here after login; the backend
 * rejects other API calls until the password is changed.
 */
export default function ChangePasswordPage() {
  const t = useTranslations("auth.changePassword");
  const router = useRouter();
  const token = useAuthStore((state) => state.token);
  const mustChangePassword = useAuthStore((state) => state.user?.mustChangePassword ?? false);
  const logout = useAuthStore((state) => state.logout);
  const session = useSession();

  useEffect(() => {
    if (session === "anonymous") {
      logout();
      router.replace("/login");
    }
  }, [session, logout, router]);

  if (session !== "authenticated" || !token) return null;

  const handleLogout = () => {
    revokeSession();
    logout();
    router.replace("/login");
  };

  return (
    <AuthShell>
      <div className="stack gap-6">
        <div>
          <h1 className="h1">{t("title")}</h1>
          <p className="small t3 mt-1.5">{mustChangePassword ? t("required") : t("optional")}</p>
        </div>

        <ChangePasswordForm onSuccess={() => router.replace("/dashboard")} />

        <button type="button" onClick={handleLogout} className="link xs t3 self-center">
          {t("logout")}
        </button>
      </div>
    </AuthShell>
  );
}
