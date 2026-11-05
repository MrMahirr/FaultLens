"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Sidebar } from "@/shared/components/layout/Sidebar";
import { Topbar } from "@/shared/components/layout/Topbar";
import { useUIStore } from "@/shared/store/ui.store";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useAuthHydrated } from "@/features/auth/hooks/useAuthHydrated";
import { GlobalNotificationListener } from "@/shared/components/layout/GlobalNotificationListener";
import { refreshSession } from "@/shared/api/session";
import { decodeJwt } from "@/shared/lib/jwt";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const token = useAuthStore((state) => state.token);
  const mustChangePassword = useAuthStore((state) => state.user?.mustChangePassword ?? false);
  const router = useRouter();
  const hydrated = useAuthHydrated();

  useEffect(() => {
    // Before hydration the store still holds its initial state (no token) — do not redirect yet.
    if (!hydrated) return;
    if (!token) {
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
  }, [hydrated, token, mustChangePassword, router]);

  // Prevent flash of content during initial SSR or before redirect
  if (!hydrated || !token || mustChangePassword) {
    return (
      <div className="min-h-screen bg-bg-primary flex items-center justify-center">
        <div className="w-10 h-10 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-primary bg-grid-pattern">
      {/* Sidebar */}
      <Sidebar />

      {/* Main Content */}
      <motion.div
        className="min-h-screen transition-theme"
        animate={{ marginLeft: sidebarOpen ? 240 : 64 }}
        transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
      >
        <GlobalNotificationListener />

        {/* Topbar */}
        <Topbar />

        {/* Page Content */}
        <main className="p-4 sm:p-6 lg:p-8">{children}</main>
      </motion.div>
    </div>
  );
}
