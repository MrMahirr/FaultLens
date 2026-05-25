"use client";

import { motion } from "framer-motion";
import { Sidebar } from "@/shared/components/layout/Sidebar";
import { Topbar } from "@/shared/components/layout/Topbar";
import { useUIStore } from "@/shared/store/ui.store";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);

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
        {/* Topbar */}
        <Topbar />

        {/* Page Content */}
        <main className="p-4 sm:p-6 lg:p-8">{children}</main>
      </motion.div>
    </div>
  );
}
