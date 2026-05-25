"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import {
  LayoutDashboard,
  ScrollText,
  Server,
  Brain,
  Rocket,
  ChevronLeft,
  ChevronRight,
  Hexagon,
  Home,
  Bell,
  Settings,
} from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { useUIStore } from "@/shared/store/ui.store";
import { Tooltip } from "@/shared/components/ui/Tooltip";

/* ── Types ─────────────────────────────────────────────────── */

interface NavItem {
  label: string;
  href: string;
  icon: React.ReactNode;
}

/* ── Nav Items ─────────────────────────────────────────────── */

const NAV_ITEMS: NavItem[] = [
  { label: "Anasayfa", href: "/", icon: <Home size={20} /> },
  { label: "Dashboard", href: "/dashboard", icon: <LayoutDashboard size={20} /> },
  { label: "Logs", href: "/logs", icon: <ScrollText size={20} /> },
  { label: "Kaynaklar", href: "/sources", icon: <Server size={20} /> },
  { label: "Analizler", href: "/analyses", icon: <Brain size={20} /> },
  { label: "Deployments", href: "/deployments", icon: <Rocket size={20} /> },
  { label: "Alarmlar", href: "/alerts", icon: <Bell size={20} /> },
  { label: "Ayarlar", href: "/settings", icon: <Settings size={20} /> },
];

/* ── Component ─────────────────────────────────────────────── */

function Sidebar() {
  const pathname = usePathname();
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);

  const isActive = (href: string) => {
    if (href === "/") return pathname === "/";
    if (href === "/dashboard") return pathname === "/dashboard";
    return pathname?.startsWith(href) ?? false;
  };

  return (
    <motion.aside
      layout
      className={cn(
        "fixed left-0 top-0 h-screen z-40",
        "bg-bg-secondary border-r border-border-default",
        "flex flex-col transition-theme",
        "overflow-hidden"
      )}
      animate={{ width: sidebarOpen ? 240 : 64 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      {/* ── Logo ───────────────────────────────────── */}
      <div className="flex items-center h-16 px-4 border-b border-border-default shrink-0">
        <Link href="/dashboard" className="flex items-center gap-2.5 overflow-hidden">
          <div className="shrink-0 w-8 h-8 rounded-lg bg-accent/10 flex items-center justify-center">
            <Hexagon size={18} className="text-accent" />
          </div>
          <motion.span
            className="font-display font-bold text-lg text-text-primary whitespace-nowrap"
            animate={{ opacity: sidebarOpen ? 1 : 0, width: sidebarOpen ? "auto" : 0 }}
            transition={{ duration: 0.2 }}
          >
            Fault<span className="text-accent">Lens</span>
          </motion.span>
        </Link>
      </div>

      {/* ── Navigation ─────────────────────────────── */}
      <nav className="flex-1 py-4 px-2 space-y-1 overflow-y-auto">
        {NAV_ITEMS.map((item) => {
          const active = isActive(item.href);

          const linkContent = (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg",
                "transition-all duration-200 group relative",
                active
                  ? "bg-accent/10 text-accent"
                  : "text-text-secondary hover:text-text-primary hover:bg-bg-tertiary"
              )}
            >
              {/* Active indicator */}
              {active && (
                <motion.div
                  layoutId="sidebar-active"
                  className="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 bg-accent rounded-full"
                  transition={{ type: "spring", damping: 20, stiffness: 300 }}
                />
              )}

              <span className="shrink-0">{item.icon}</span>

              <motion.span
                className="text-sm font-medium whitespace-nowrap overflow-hidden"
                animate={{
                  opacity: sidebarOpen ? 1 : 0,
                  width: sidebarOpen ? "auto" : 0,
                }}
                transition={{ duration: 0.2 }}
              >
                {item.label}
              </motion.span>
            </Link>
          );

          // Show tooltip when sidebar is collapsed
          if (!sidebarOpen) {
            return (
              <Tooltip key={item.href} content={item.label} position="right">
                {linkContent}
              </Tooltip>
            );
          }

          return <div key={item.href}>{linkContent}</div>;
        })}
      </nav>

      {/* ── Collapse Toggle ────────────────────────── */}
      <div className="p-2 border-t border-border-default shrink-0">
        <button
          onClick={toggleSidebar}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-tertiary transition-colors"
          aria-label={sidebarOpen ? "Sidebar'ı kapat" : "Sidebar'ı aç"}
        >
          {sidebarOpen ? <ChevronLeft size={18} /> : <ChevronRight size={18} />}
          <motion.span
            className="text-xs whitespace-nowrap overflow-hidden"
            animate={{ opacity: sidebarOpen ? 1 : 0, width: sidebarOpen ? "auto" : 0 }}
            transition={{ duration: 0.2 }}
          >
            Daralt
          </motion.span>
        </button>
      </div>
    </motion.aside>
  );
}

export { Sidebar };
