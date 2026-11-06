"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Plus } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { AlarmStats } from "@/features/alarms/components/AlarmStats";
import { AlarmsList } from "@/features/alarms/components/AlarmsList";
import { AlarmRulesList } from "@/features/alarms/components/AlarmRulesList";
import { CreateRuleModal } from "@/features/alarms/components/CreateRuleModal";
import { Button } from "@/shared/components/ui/Button";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Tabs } from "@/shared/components/ui/Tabs";

type Tab = "alarms" | "rules";

export default function AlarmsPage() {
  const t = useTranslations("alarms");
  const [activeTab, setActiveTab] = useState<Tab>("alarms");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const canEdit = useCan("edit");

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        action={
          canEdit && (
            <Button
              variant="primary"
              icon={<Plus size={16} aria-hidden="true" />}
              onClick={() => setIsModalOpen(true)}
            >
              {t("newRule")}
            </Button>
          )
        }
      />

      <AlarmStats />

      <Tabs
        label={t("tabs.label")}
        value={activeTab}
        onChange={setActiveTab}
        items={[
          { value: "alarms", label: t("tabs.alarms") },
          { value: "rules", label: t("tabs.rules") },
        ]}
        idPrefix="alarms"
      />

      <div
        role="tabpanel"
        id={`alarms-panel-${activeTab}`}
        aria-labelledby={`alarms-tab-${activeTab}`}
      >
        {activeTab === "alarms" ? <AlarmsList /> : <AlarmRulesList />}
      </div>

      <CreateRuleModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </>
  );
}
