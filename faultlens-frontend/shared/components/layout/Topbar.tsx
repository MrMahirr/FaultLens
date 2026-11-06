"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Menu, LogOut, User, Bell } from "lucide-react";
import { revokeSession } from "@/shared/api/session";
import { useUIStore } from "@/shared/store/ui.store";
import { Dropdown } from "@/shared/components/ui/Dropdown";
import { LocaleSync } from "@/shared/components/layout/LocaleSync";
import { LanguageMenu, ThemeMenu } from "@/shared/components/layout/PreferenceMenus";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useAlarmStatsQuery } from "@/features/alarms/api/useAlarmsQuery";

/* ── Breadcrumb ────────────────────────────────────────────── */

const SECTION_KEYS = {
  dashboard: "dashboard",
  logs: "logs",
  sources: "sources",
  analyses: "analyses",
  deployments: "deployments",
  alarms: "alarms",
  settings: "settings",
} as const;

/* ── Component ─────────────────────────────────────────────── */

function Topbar() {
  const t = useTranslations("shell");
  const tNav = useTranslations("nav");
  const pathname = usePathname() ?? "/dashboard";
  const router = useRouter();
  const toggleSidebar = useUIStore((state) => state.toggleSidebar);
  const user = useAuthStore((state) => state.user);
  const { data: alarmStats } = useAlarmStatsQuery();
  const activeAlarms = alarmStats?.active ?? 0;

  const [, section, ...rest] = pathname.split("/");
  const sectionKey = SECTION_KEYS[section as keyof typeof SECTION_KEYS];

  const handleUserMenu = (value: string) => {
    if (value === "profile") router.push("/settings?tab=account");
    if (value === "logout") {
      revokeSession();
      useAuthStore.getState().logout();
      // Full reload: drops in-memory query caches and sockets of the signed-out user.
      window.location.assign("/login");
    }
  };

  return (
    <header className="topbar sticky top-0 z-30 bg-bg-primary/90 backdrop-blur-md">
      <LocaleSync />

      <div className="flex items-center gap-2 min-w-0">
        <button
          type="button"
          onClick={toggleSidebar}
          className="btn btn-ghost btn-sm btn-icon lg:hidden"
          aria-label={t("toggleMenu")}
        >
          <Menu size={16} aria-hidden="true" />
        </button>

        <nav aria-label={t("breadcrumb")} className="crumbs">
          <span>FaultLens</span>
          <span aria-hidden="true">/</span>
          {sectionKey ? (
            rest.length > 0 ? (
              <>
                <Link href={`/${section}`}>{tNav(sectionKey)}</Link>
                <span aria-hidden="true">/</span>
                <b aria-current="page" className="mono">
                  {decodeURIComponent(rest.join("/"))}
                </b>
              </>
            ) : (
              <b aria-current="page">{tNav(sectionKey)}</b>
            )
          ) : (
            <b aria-current="page">{section}</b>
          )}
        </nav>
      </div>

      <div className="flex items-center gap-1">
        <LanguageMenu />
        <ThemeMenu />

        {/* Alarms */}
        <Link
          href="/alarms"
          className="btn btn-ghost btn-sm btn-icon relative"
          aria-label={activeAlarms > 0 ? t("activeAlarms", { count: activeAlarms }) : t("alarms")}
        >
          <Bell size={16} aria-hidden="true" />
          {activeAlarms > 0 && (
            <span
              className="dot absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-critical"
              aria-hidden="true"
            />
          )}
        </Link>

        {/* User */}
        <Dropdown
          label={t("userMenu")}
          trigger={
            <button
              type="button"
              className="btn btn-ghost btn-sm gap-2 pl-1"
              aria-label={t("userMenu")}
            >
              <span className="avatar w-6 h-6 text-[10px]" aria-hidden="true">
                {(user?.username ?? "?").slice(0, 1).toUpperCase()}
              </span>
              <span className="hidden md:inline small t2">{user?.username}</span>
            </button>
          }
          items={[
            { label: t("profile"), value: "profile", icon: <User size={14} /> },
            { label: t("logout"), value: "logout", icon: <LogOut size={14} />, danger: true },
          ]}
          onSelect={handleUserMenu}
        />
      </div>
    </header>
  );
}

export { Topbar };
