"use client";

import { useState } from "react";
import { Palette, User, Bell, Settings2 } from "lucide-react";
import { AppearanceSettings } from "@/features/settings/components/AppearanceSettings";
import { AccountSettings } from "@/features/settings/components/AccountSettings";
import { NotificationSettings } from "@/features/settings/components/NotificationSettings";
import { SystemSettings } from "@/features/settings/components/SystemSettings";

type TabId = "appearance" | "account" | "notifications" | "system";

interface TabDefinition {
  id: TabId;
  label: string;
  icon: React.ReactNode;
  component: React.ReactNode;
}

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<TabId>("appearance");

  const TABS: TabDefinition[] = [
    {
      id: "appearance",
      label: "Görünüm",
      icon: <Palette size={18} />,
      component: <AppearanceSettings />,
    },
    {
      id: "account",
      label: "Hesap",
      icon: <User size={18} />,
      component: <AccountSettings />,
    },
    {
      id: "notifications",
      label: "Bildirimler",
      icon: <Bell size={18} />,
      component: <NotificationSettings />,
    },
    {
      id: "system",
      label: "Sistem",
      icon: <Settings2 size={18} />,
      component: <SystemSettings />,
    },
  ];

  return (
    <div className="flex flex-col gap-6 w-full max-w-5xl mx-auto p-4 md:p-8 animate-fade-in">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-text-primary">
          Ayarlar
        </h1>
        <p className="text-text-muted mt-1">
          Kişisel tercihlerinizi ve sistem yapılandırmasını yönetin.
        </p>
      </div>

      <div className="flex flex-col md:flex-row gap-8 mt-4">
        {/* Horizontal / Vertical Tabs depending on screen size */}
        <div className="md:w-64 shrink-0">
          <nav className="flex md:flex-col gap-1 overflow-x-auto md:overflow-visible pb-2 md:pb-0">
            {TABS.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 whitespace-nowrap cursor-pointer ${
                    isActive
                      ? "bg-accent/10 text-accent font-medium"
                      : "text-text-secondary hover:bg-bg-tertiary hover:text-text-primary"
                  }`}
                >
                  <span className={isActive ? "text-accent" : "text-text-muted"}>
                    {tab.icon}
                  </span>
                  {tab.label}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Content Area */}
        <div className="flex-1 bg-bg-secondary p-6 rounded-3xl border border-border-default min-h-[500px]">
          {TABS.find((t) => t.id === activeTab)?.component}
        </div>
      </div>
    </div>
  );
}
