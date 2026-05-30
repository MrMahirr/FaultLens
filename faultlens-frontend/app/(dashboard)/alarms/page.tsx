"use client";

import { useState } from "react";
import { AlarmStats } from "@/features/alarms/components/AlarmStats";
import { AlarmsList } from "@/features/alarms/components/AlarmsList";
import { AlarmRulesList } from "@/features/alarms/components/AlarmRulesList";
import { CreateRuleModal } from "@/features/alarms/components/CreateRuleModal";
import { Button } from "@/shared/components/ui/Button";
import { Plus } from "lucide-react";

type Tab = "alarms" | "rules";

export default function AlarmsPage() {
  const [activeTab, setActiveTab] = useState<Tab>("alarms");
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className="flex flex-col gap-6 w-full max-w-7xl mx-auto p-4 md:p-8 animate-fade-in">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-text-primary">
            Alarmlar & Kurallar
          </h1>
          <p className="text-text-muted mt-1">
            Sistem durumunu, alarmları ve tetikleyici kuralları yönetin.
          </p>
        </div>
        <Button
          variant="primary"
          icon={<Plus size={16} />}
          onClick={() => setIsModalOpen(true)}
        >
          Yeni Kural Ekle
        </Button>
      </div>

      <AlarmStats />

      <div className="flex gap-2 border-b border-border-default pb-0 mb-4">
        <button
          onClick={() => setActiveTab("alarms")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors duration-200 cursor-pointer ${
            activeTab === "alarms"
              ? "border-accent text-accent"
              : "border-transparent text-text-muted hover:text-text-primary"
          }`}
        >
          Aktif Alarmlar
        </button>
        <button
          onClick={() => setActiveTab("rules")}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors duration-200 cursor-pointer ${
            activeTab === "rules"
              ? "border-accent text-accent"
              : "border-transparent text-text-muted hover:text-text-primary"
          }`}
        >
          Alarm Kuralları
        </button>
      </div>

      <div className="bg-bg-secondary p-6 rounded-2xl border border-border-default">
        {activeTab === "alarms" ? <AlarmsList /> : <AlarmRulesList />}
      </div>

      <CreateRuleModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
}
