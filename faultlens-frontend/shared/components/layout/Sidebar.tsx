"use client";

import { useEffect, useRef } from "react";
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
  X,
} from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { useUIStore } from "@/shared/store/ui.store";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { Tooltip } from "@/shared/components/ui/Tooltip";
import { BrandMark } from "@/shared/components/layout/BrandMark";
import { useFocusTrap } from "@/shared/hooks/useFocusTrap";
import { DESKTOP_QUERY, useMediaQuery } from "@/shared/hooks/useMediaQuery";

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

/**
 * lg and up: a fixed rail, expanded (240px) or icon-only (64px), persisted.
 * Below lg: an off-canvas drawer over a scrim (always expanded), opened from the Topbar; it
 * traps focus, closes on Esc, on the scrim and after navigating, and is inert while closed.
 */
function Sidebar() {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const sidebarOpen = useUIStore((state) => state.sidebarOpen);
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);
  const mobileNavOpen = useUIStore((state) => state.mobileNavOpen);
  const setMobileNavOpen = useUIStore((state) => state.setMobileNavOpen);
  const user = useAuthStore((state) => state.user);
  // Server render assumes desktop: the drawer is closed there anyway, so nothing flashes.
  const isDesktop = useMediaQuery(DESKTOP_QUERY, true);
  const drawerOpen = !isDesktop && mobileNavOpen;
  const expanded = isDesktop ? sidebarOpen : true;
  const asideRef = useRef<HTMLElement>(null);

  useFocusTrap(asideRef, drawerOpen);

  // A link was followed, or the window grew past lg: the drawer has done its job.
  useEffect(() => {
    setMobileNavOpen(false);
  }, [pathname, isDesktop, setMobileNavOpen]);

  useEffect(() => {
    if (!drawerOpen) return;
    const onKeyDown = (e: KeyboardEvent) => e.key === "Escape" && setMobileNavOpen(false);
    document.addEventListener("keydown", onKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [drawerOpen, setMobileNavOpen]);

  const isActive = (href: string) =>
    href === "/dashboard" ? pathname === href : (pathname?.startsWith(href) ?? false);

  const renderItem = (item: NavItem) => {
    const active = isActive(item.href);
    const link = (
      <Link
        href={item.href}
        className={cn("nav", !expanded && "justify-center px-0 w-10")}
        aria-current={active ? "page" : undefined}
        aria-label={expanded ? undefined : t(item.key)}
      >
        <span aria-hidden="true" className="inline-flex">
          {item.icon}
        </span>
        {expanded && <span className="truncate">{t(item.key)}</span>}
      </Link>
    );

    return expanded ? (
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
    <>
      {drawerOpen && (
        <button
          type="button"
          className="drawer-scrim lg:hidden"
          tabIndex={-1}
          aria-hidden="true"
          onClick={() => setMobileNavOpen(false)}
        />
      )}

      <aside
        ref={asideRef}
        id="app-sidebar"
        aria-label={t("label")}
        // Off-screen drawer: out of the tab order and the accessibility tree.
        inert={!isDesktop && !mobileNavOpen}
        role={drawerOpen ? "dialog" : undefined}
        aria-modal={drawerOpen || undefined}
        className={cn(
          "fixed left-0 top-0 h-dvh z-40 flex flex-col gap-0.5 py-3",
          "bg-bg-tertiary border-r border-border-default overflow-hidden",
          "transition-[width,transform] duration-200 ease-[cubic-bezier(.2,0,0,1)]",
          expanded ? "w-60 px-2.5" : "w-16 px-3 items-center",
          // Drawer position below lg; always in place from lg up.
          mobileNavOpen ? "translate-x-0 shadow-lg" : "-translate-x-full",
          "lg:translate-x-0 lg:shadow-none",
        )}
      >
        {/* Brand (+ close on phones) */}
        <div className={cn("flex items-center mb-1", expanded && "justify-between")}>
          <Link href="/dashboard" className="brand" aria-label="FaultLens">
            <BrandMark />
            {expanded && <span>FaultLens</span>}
          </Link>
          {!isDesktop && (
            <button
              type="button"
              className="btn btn-ghost btn-sm btn-icon"
              aria-label={t("close")}
              onClick={() => setMobileNavOpen(false)}
            >
              <X size={16} aria-hidden="true" />
            </button>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto w-full" aria-label={t("main")}>
          {GROUPS.map((group) => (
            <div key={group.key}>
              {expanded ? (
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

        {user && expanded && (
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

        {/* Rail width toggle: desktop only (the drawer has its own close button). */}
        {isDesktop && (
          <button
            type="button"
            onClick={toggleSidebar}
            className={cn("nav mt-1 text-text-muted", !expanded && "justify-center px-0 w-10")}
            aria-label={expanded ? t("collapse") : t("expand")}
            aria-expanded={expanded}
          >
            {expanded ? (
              <ChevronsLeft size={16} aria-hidden="true" />
            ) : (
              <ChevronsRight size={16} aria-hidden="true" />
            )}
            {expanded && <span className="xs">{t("collapse")}</span>}
          </button>
        )}
      </aside>
    </>
  );
}

export { Sidebar };
