"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Palette, User, Bell, Settings2, Users, KeyRound } from "lucide-react";
import { AppearanceSettings } from "@/features/settings/components/AppearanceSettings";
import { AccountSettings } from "@/features/settings/components/AccountSettings";
import { NotificationSettings } from "@/features/settings/components/NotificationSettings";
import { SystemSettings } from "@/features/settings/components/SystemSettings";
import { UserManagement } from "@/features/settings/components/UserManagement";
import { ApiKeySettings } from "@/features/apiKeys/components/ApiKeySettings";
import { useCan } from "@/features/auth/hooks/useCan";
import { PageHeader } from "@/shared/components/layout/PageHeader";

const TAB_IDS = ["appearance", "account", "notifications", "system", "users", "apiKeys"] as const;
type TabId = (typeof TAB_IDS)[number];

function isTabId(value: string | null): value is TabId {
  return value !== null && (TAB_IDS as readonly string[]).includes(value);
}

interface TabDefinition {
  id: TabId;
  icon: React.ReactNode;
  component: React.ReactNode;
  /** Hidden from non-admins (the API rejects them anyway). */
  adminOnly?: boolean;
}

const ALL_TABS: TabDefinition[] = [
  { id: "appearance", icon: <Palette size={16} />, component: <AppearanceSettings /> },
  { id: "account", icon: <User size={16} />, component: <AccountSettings /> },
  { id: "notifications", icon: <Bell size={16} />, component: <NotificationSettings /> },
  { id: "system", icon: <Settings2 size={16} />, component: <SystemSettings />, adminOnly: true },
  { id: "users", icon: <Users size={16} />, component: <UserManagement />, adminOnly: true },
  { id: "apiKeys", icon: <KeyRound size={16} />, component: <ApiKeySettings />, adminOnly: true },
];

function SettingsContent() {
  const t = useTranslations("settings");
  const tCommon = useTranslations("common");
  const searchParams = useSearchParams();
  const tabQuery = searchParams?.get("tab") ?? null;
  const isAdmin = useCan("manageUsers");

  const tabs = ALL_TABS.filter((tab) => isAdmin || !tab.adminOnly);
  // The URL is the single source of truth for the selected section (shareable link).
  const activeTab: TabId =
    isTabId(tabQuery) && tabs.some((tab) => tab.id === tabQuery) ? tabQuery : "appearance";

  return (
    <>
      <PageHeader title={t("title")} description={t("subtitle")} />

      <div className="flex flex-col md:flex-row gap-6">
        <nav className="md:w-56 shrink-0" aria-label={t("sections")}>
          <ul className="flex md:flex-col gap-0.5 overflow-x-auto m-0 p-0 list-none">
            {tabs.map((tab) => (
              <li key={tab.id}>
                <Link
                  href={`/settings?tab=${tab.id}`}
                  replace
                  scroll={false}
                  className="nav whitespace-nowrap"
                  aria-current={activeTab === tab.id ? "page" : undefined}
                >
                  <span aria-hidden="true" className="inline-flex">
                    {tab.icon}
                  </span>
                  {t(`tabs.${tab.id}`)}
                  {tab.adminOnly && (
                    <span
                      className="badge mono ol ml-auto text-[10px]"
                      title={tCommon("adminOnly")}
                    >
                      ADMIN
                    </span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <section className="flex-1 min-w-0 swA" key={activeTab} aria-label={t(`tabs.${activeTab}`)}>
          {tabs.find((tab) => tab.id === activeTab)?.component}
        </section>
      </div>
    </>
  );
}

export default function SettingsPage() {
  return (
    <Suspense>
      <SettingsContent />
    </Suspense>
  );
}
