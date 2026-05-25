"use client";

import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import { Menu, Sun, Moon, LogOut, User } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { useUIStore } from "@/shared/store/ui.store";
import { useTheme } from "@/shared/hooks/useTheme";
import { Dropdown, type DropdownItem } from "@/shared/components/ui/Dropdown";

/* ── Breadcrumb Map ────────────────────────────────────────── */

const ROUTE_LABELS: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/logs": "Logs",
  "/sources": "Sources",
  "/analyses": "Analyses",
  "/deployments": "Deployments",
};

/* ── Component ─────────────────────────────────────────────── */

function Topbar() {
  const pathname = usePathname();
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const { theme, toggleTheme } = useTheme();

  // Build breadcrumb from pathname
  const breadcrumb = pathname
    ? ROUTE_LABELS[pathname] ??
      pathname
        .split("/")
        .filter(Boolean)
        .map((seg) => seg.charAt(0).toUpperCase() + seg.slice(1))
        .join(" / ")
    : "Dashboard";

  const userMenuItems: DropdownItem[] = [
    { label: "Profil", value: "profile", icon: <User size={14} /> },
    { label: "Çıkış Yap", value: "logout", icon: <LogOut size={14} />, danger: true },
  ];

  const handleUserMenuSelect = (value: string) => {
    switch (value) {
      case "logout":
        // Will be connected to auth store in Step 5
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
        break;
      default:
        break;
    }
  };

  return (
    <header
      className={cn(
        "sticky top-0 z-30 h-16",
        "bg-bg-primary/80 backdrop-blur-lg",
        "border-b border-border-default",
        "flex items-center justify-between px-4 gap-4",
        "transition-theme"
      )}
    >
      {/* Left: Hamburger + Breadcrumb */}
      <div className="flex items-center gap-3">
        <button
          onClick={toggleSidebar}
          className="p-2 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary transition-colors lg:hidden"
          aria-label="Menüyü Aç/Kapat"
        >
          <Menu size={20} />
        </button>

        <div className="hidden sm:flex items-center gap-2 text-sm">
          <span className="text-text-muted">LogLens</span>
          <span className="text-text-muted">/</span>
          <span className="text-text-primary font-medium">{breadcrumb}</span>
        </div>
      </div>

      {/* Right: Theme Toggle + User */}
      <div className="flex items-center gap-2">
        {/* Theme Toggle */}
        <button
          onClick={toggleTheme}
          className="p-2 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary transition-colors"
          aria-label={theme === "dark" ? "Açık temaya geç" : "Koyu temaya geç"}
        >
          <motion.div
            key={theme}
            initial={{ rotate: -90, opacity: 0 }}
            animate={{ rotate: 0, opacity: 1 }}
            exit={{ rotate: 90, opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
          </motion.div>
        </button>

        {/* User Menu */}
        <Dropdown
          trigger={
            <button className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-bg-tertiary transition-colors">
              <div className="w-7 h-7 rounded-full bg-accent/20 flex items-center justify-center">
                <span className="text-xs font-bold text-accent">A</span>
              </div>
              {sidebarOpen && (
                <span className="text-sm text-text-secondary hidden md:block">
                  Admin
                </span>
              )}
            </button>
          }
          items={userMenuItems}
          onSelect={handleUserMenuSelect}
        />
      </div>
    </header>
  );
}

export { Topbar };
