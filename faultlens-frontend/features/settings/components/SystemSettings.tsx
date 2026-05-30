"use client";

import { useState, useEffect, useCallback } from "react";
import { Database, BrainCircuit, Globe, HardDrive } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";

export function SystemSettings() {
  const [retention, setRetention] = useState("30");
  const [engine, setEngine] = useState("ai");
  const [language, setLanguage] = useState("tr");

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const fetchSettings = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await apiClient.get(Endpoints.SETTINGS.SYSTEM);
      const data = response.data?.data;
      if (data) {
        setRetention(String(data.logRetentionDays ?? 30));
        setEngine(data.defaultEngine ?? "ai");
        setLanguage(data.language ?? "tr");
      }
    } catch {
      // Fallback to defaults
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const response = await apiClient.put(Endpoints.SETTINGS.SYSTEM, {
        logRetentionDays: parseInt(retention, 10),
        defaultEngine: engine,
        language,
      });
      const data = response.data?.data;
      if (data) {
        setRetention(String(data.logRetentionDays));
        setEngine(data.defaultEngine);
        setLanguage(data.language);
      }
      toast.success("Sistem tercihleri başarıyla kaydedildi!");
    } catch (err: any) {
      const message =
        err.response?.data?.message || "Ayarlar kaydedilirken bir hata oluştu.";
      toast.error(message);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="flex items-center justify-between">
          <div className="h-10 w-48 bg-bg-tertiary/30 rounded" />
          <div className="h-8 w-32 bg-bg-tertiary/30 rounded" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="h-32 bg-bg-tertiary/30 rounded-2xl border border-border-default" />
          <div className="h-32 bg-bg-tertiary/30 rounded-2xl border border-border-default" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-text-primary">
            Sistem Tercihleri
          </h2>
          <p className="text-sm text-text-muted mt-1">
            Uygulamanın veri saklama, dil ve analiz motoru ayarlarını yapılandırın.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="primary"
            size="sm"
            onClick={handleSave}
            loading={isSaving}
          >
            Değişiklikleri Kaydet
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Retention Settings */}
        <div className="bg-bg-tertiary/30 p-6 rounded-2xl border border-border-default space-y-4">
          <div className="flex items-center gap-3 border-b border-border-default pb-3">
            <HardDrive size={18} className="text-text-secondary" />
            <h3 className="font-medium text-text-primary">Log Saklama Süresi</h3>
          </div>
          <p className="text-sm text-text-muted">
            Veritabanındaki logların ne kadar süreyle tutulacağını belirleyin.
          </p>
          <select
            className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2.5 text-sm focus:outline-none focus:border-accent cursor-pointer"
            value={retention}
            onChange={(e) => setRetention(e.target.value)}
          >
            <option value="7">7 Gün</option>
            <option value="15">15 Gün</option>
            <option value="30">30 Gün</option>
            <option value="90">90 Gün</option>
            <option value="0">Süresiz (Silme)</option>
          </select>
        </div>

        {/* Analysis Engine */}
        <div className="bg-bg-tertiary/30 p-6 rounded-2xl border border-border-default space-y-4">
          <div className="flex items-center gap-3 border-b border-border-default pb-3">
            <BrainCircuit size={18} className="text-text-secondary" />
            <h3 className="font-medium text-text-primary">Varsayılan Analiz Motoru</h3>
          </div>
          <p className="text-sm text-text-muted">
            Logları analiz ederken kullanılacak varsayılan algoritmayı seçin.
          </p>
          <select
            className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2.5 text-sm focus:outline-none focus:border-accent cursor-pointer"
            value={engine}
            onChange={(e) => setEngine(e.target.value)}
          >
            <option value="rule">Rule-Based (Kural Tabanlı)</option>
            <option value="ai">AI Analysis (Yapay Zeka Destekli)</option>
          </select>
        </div>

        {/* Language */}
        <div className="bg-bg-tertiary/30 p-6 rounded-2xl border border-border-default space-y-4 md:col-span-2 lg:col-span-1">
          <div className="flex items-center gap-3 border-b border-border-default pb-3">
            <Globe size={18} className="text-text-secondary" />
            <h3 className="font-medium text-text-primary">Arayüz Dili</h3>
          </div>
          <p className="text-sm text-text-muted">Sistem dilini değiştirin.</p>
          <select
            className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2.5 text-sm focus:outline-none focus:border-accent cursor-pointer"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
          >
            <option value="tr">Türkçe</option>
            <option value="en">English (Yakında)</option>
          </select>
        </div>
      </div>
    </div>
  );
}
