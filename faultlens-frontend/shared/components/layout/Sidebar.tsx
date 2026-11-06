"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  LayoutDashboard,
  ScrollText,
  Server,
  Brain,
  Rocket,
  ChevronsLeft,
  ChevronsRight,
  Bell,
  Settings,
  Hexagon,
} from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { useUIStore } from "@/shared/store/ui.store";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { Tooltip } from "@/shared/components/ui/Tooltip";

/* ── Nav model (Instrument: Gözlem · Altyapı · Yanıt) ──────── */

type NavKey = "dashboard" | "logs" | "analyses" | "sources" | "deployments" | "alarms" | "settings";

interface NavItem {
  key: NavKey;
  href: string;
  icon: React.ReactNode;
}

const GROUPS: { key: "observe" | "infra" | "respond"; items: NavItem[] }[] = [
  {
    key: "observe",
    items: [
      { key: "dashboard", href: "/dashboard", icon: <LayoutDashboard size={16} /> },
      { key: "logs", href: "/logs", icon: <ScrollText size={16} /> },
      { key: "analyses", href: "/analyses", icon: <Brain size={16} /> },
    ],
  },
  {
    key: "infra",
    items: [
      { key: "sources", href: "/sources", icon: <Server size={16} /> },
      { key: "deployments", href: "/deployments", icon: <Rocket size={16} /> },
    ],
  },
  {
    key: "respond",
    items: [{ key: "alarms", href: "/alarms", icon: <Bell size={16} /> }],
  },
];

const SETTINGS_ITEM: NavItem = {
  key: "settings",
  href: "/settings",
  icon: <Settings size={16} />,
};

/* ── Component ─────────────────────────────────────────────── */

function Sidebar() {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);
  const user = useAuthStore((state) => state.user);

  const isActive = (href: string) =>
    href === "/dashboard" ? pathname === href : (pathname?.startsWith(href) ?? false);

  const renderItem = (item: NavItem) => {
    const active = isActive(item.href);
    const link = (
      <Link
        href={item.href}
        className={cn("nav", !sidebarOpen && "justify-center px-0 w-10")}
        aria-current={active ? "page" : undefined}
        aria-label={sidebarOpen ? undefined : t(item.key)}
      >
        <span aria-hidden="true" className="inline-flex">
          {item.icon}
        </span>
        {sidebarOpen && <span className="truncate">{t(item.key)}</span>}
      </Link>
    );

    return sidebarOpen ? (
      <li key={item.href}>{link}</li>
    ) : (
      <li key={item.href}>
        <Tooltip content={t(item.key)} position="right">
          {link}
        </Tooltip>
      </li>
    );
  };

  return (
    <aside
      aria-label={t("label")}
      className={cn(
        "fixed left-0 top-0 h-screen z-40 flex flex-col gap-0.5 py-3",
        "bg-bg-tertiary border-r border-border-default overflow-hidden",
        "transition-[width] duration-200 ease-[cubic-bezier(.2,0,0,1)]",
        sidebarOpen ? "w-60 px-2.5" : "w-16 px-3 items-center",
      )}
    >
      {/* Brand */}
      <Link href="/dashboard" className="brand mb-1" aria-label="FaultLens">
        <span
          className="w-6 h-6 rounded-[6px] bg-primary text-on-primary grid place-items-center shrink-0"
          aria-hidden="true"
        >
          <Hexagon size={14} strokeWidth={2.2} />
        </span>
        {sidebarOpen && <span>FaultLens</span>}
      </Link>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto w-full" aria-label={t("main")}>
        {GROUPS.map((group) => (
          <div key={group.key}>
            {sidebarOpen ? (
              <p className="nav-group">{t(`groups.${group.key}`)}</p>
            ) : (
              <div className="hr my-2" aria-hidden="true" />
            )}
            <ul className="flex flex-col gap-0.5">{group.items.map(renderItem)}</ul>
          </div>
        ))}
      </nav>

      {/* Settings + user */}
      <ul className="flex flex-col gap-0.5 w-full">{renderItem(SETTINGS_ITEM)}</ul>

      {user && sidebarOpen && (
        <div className="mt-2 flex items-center gap-2.5 px-2 py-2 rounded-md border border-border-default bg-bg-secondary">
          <span className="avatar" aria-hidden="true">
            {user.username.slice(0, 1).toUpperCase()}
          </span>
          <div className="min-w-0 flex-1">
            <p className="small font-medium truncate">{user.username}</p>
          </div>
          <span className="badge mono ol">{user.role}</span>
        </div>
      )}

      <button
        type="button"
        onClick={toggleSidebar}
        className={cn("nav mt-1 text-text-muted", !sidebarOpen && "justify-center px-0 w-10")}
        aria-label={sidebarOpen ? t("collapse") : t("expand")}
        aria-expanded={sidebarOpen}
      >
        {sidebarOpen ? (
          <ChevronsLeft size={16} aria-hidden="true" />
        ) : (
          <ChevronsRight size={16} aria-hidden="true" />
        )}
        {sidebarOpen && <span className="xs">{t("collapse")}</span>}
      </button>
    </aside>
  );
}

export { Sidebar };
