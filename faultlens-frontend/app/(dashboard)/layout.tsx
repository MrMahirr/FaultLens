"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Sidebar } from "@/shared/components/layout/Sidebar";
import { Topbar } from "@/shared/components/layout/Topbar";
import { useUIStore } from "@/shared/store/ui.store";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useSession } from "@/features/auth/hooks/useSession";
import { GlobalNotificationListener } from "@/shared/components/layout/GlobalNotificationListener";
import { refreshSession } from "@/shared/api/session";
import { decodeJwt } from "@/shared/lib/jwt";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const token = useAuthStore((state) => state.token);
  const mustChangePassword = useAuthStore((state) => state.user?.mustChangePassword ?? false);
  const router = useRouter();
  const pathname = usePathname();
  const session = useSession();
  const t = useTranslations("shell");

  useEffect(() => {
    // Before hydration / while the session is restored there is no token yet — do not redirect.
    if (session === "loading") return;
    if (session === "anonymous" || !token) {
      // Also clears the session cookies, so proxy.ts does not bounce /login back here.
      useAuthStore.getState().logout();
      router.replace("/login");
    } else if (mustChangePassword) {
      router.replace("/change-password");
    } else {
      // Back after the 15-minute access token expired: renew before the WebSocket and queries
      // use it (a failed renewal ends at /login via the API client).
      const exp = decodeJwt(token)?.exp;
      if (exp && exp * 1000 <= Date.now()) void refreshSession();
    }
  }, [session, token, mustChangePassword, router]);

  // Prevent flash of content during initial SSR or before redirect
  if (session !== "authenticated" || !token || mustChangePassword) {
    return (
      <div className="min-h-screen bg-bg-primary flex items-center justify-center" role="status">
        <span className="spinner w-6 h-6 text-text-muted" aria-hidden="true" />
        <span className="sr">{t("loadingSession")}</span>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-primary">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 btn btn-primary"
      >
        {t("skipToContent")}
      </a>

      <Sidebar />

      {/* Below lg the sidebar is a drawer over the content, so no margin is reserved. */}
      <div
        className={
          "min-h-screen flex flex-col transition-[margin] duration-200 " +
          (sidebarOpen ? "lg:ml-60" : "lg:ml-16")
        }
      >
        <GlobalNotificationListener />
        <Topbar />
        {/* Keyed by route: each page fades in instead of swapping abruptly. */}
        <main
          key={pathname}
          id="main"
          tabIndex={-1}
          className="page page-enter mx-auto outline-none"
        >
          {children}
        </main>
      </div>
    </div>
  );
}
