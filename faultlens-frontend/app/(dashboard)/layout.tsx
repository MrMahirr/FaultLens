"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Sidebar } from "@/shared/components/layout/Sidebar";
import { Topbar } from "@/shared/components/layout/Topbar";
import { useUIStore } from "@/shared/store/ui.store";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { GlobalNotificationListener } from "@/shared/components/layout/GlobalNotificationListener";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const token = useAuthStore((state) => state.token);
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (!token) {
      router.replace("/login");
    }
  }, [token, router]);

  // Prevent flash of content during initial SSR or before redirect
  if (!mounted || !token) {
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
