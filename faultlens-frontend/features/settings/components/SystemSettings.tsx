"use client";

import { useState, useEffect } from "react";
import { Database, BrainCircuit, HardDrive } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";

export function SystemSettings() {
  const [retention, setRetention] = useState("30");
  const [engine, setEngine] = useState("ai");
  const [language, setLanguage] = useState("tr");
  // Whether the backend can run AI analysis at all (AI_ENABLED + API key).
  const [aiAvailable, setAiAvailable] = useState(false);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Load once on mount; state is only set in promise callbacks and not after unmount.
  useEffect(() => {
    let active = true;
    apiClient
      .get(Endpoints.SETTINGS.SYSTEM)
      .then((response) => {
        const data = response.data?.data;
        if (active && data) {
          setRetention(String(data.logRetentionDays ?? 30));
          setEngine(data.defaultEngine ?? "ai");
          setLanguage(data.language ?? "tr");
          setAiAvailable(Boolean(data.aiAvailable));
        }
      })
      .catch(() => {
        // Fallback to defaults
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

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
        setAiAvailable(Boolean(data.aiAvailable));
      }
      toast.success("Sistem tercihleri başarıyla kaydedildi!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Ayarlar kaydedilirken bir hata oluştu."));
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
          <h2 className="text-lg font-semibold text-text-primary">Sistem Tercihleri</h2>
          <p className="text-sm text-text-muted mt-1">
            Uygulamanın veri saklama ve analiz motoru ayarlarını yapılandırın.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="primary" size="sm" onClick={handleSave} loading={isSaving}>
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
            <option value="120">120 Gün (4 ay)</option>
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
            <option value="ai" disabled={!aiAvailable && engine !== "ai"}>
              AI Analysis (Yapay Zeka Destekli){aiAvailable ? "" : " — devre dışı"}
            </option>
          </select>
          {!aiAvailable && (
            <p className="text-xs text-warning">
              Yapay zeka analizi bu sunucuda kapalı (AI_ENABLED / OPENAI_API_KEY tanımlı değil).
              {engine === "ai" && " Seçim “AI” olsa da analizler kural tabanlı motorla yapılıyor."}
            </p>
          )}
        </div>

        {/* Language: hidden until the UI is translated (P09 i18n); the stored value is kept. */}
      </div>
    </div>
  );
}
