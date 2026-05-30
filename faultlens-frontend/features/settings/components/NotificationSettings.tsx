"use client";

import { useState, useEffect, useCallback } from "react";
import { Mail, Hash, BellRing, Smartphone } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";

interface NotificationOption {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  field: "emailEnabled" | "slackEnabled" | "pushEnabled" | "mobileEnabled";
  enabled: boolean;
}

interface NotificationState {
  emailEnabled: boolean;
  slackEnabled: boolean;
  slackWebhook: string | null;
  pushEnabled: boolean;
  mobileEnabled: boolean;
}

const DEFAULT_STATE: NotificationState = {
  emailEnabled: true,
  slackEnabled: false,
  slackWebhook: null,
  pushEnabled: false,
  mobileEnabled: false,
};

export function NotificationSettings() {
  const [settings, setSettings] = useState<NotificationState>(DEFAULT_STATE);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const fetchSettings = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await apiClient.get(Endpoints.SETTINGS.NOTIFICATIONS);
      const data = response.data?.data;
      if (data) {
        setSettings({
          emailEnabled: data.emailEnabled ?? true,
          slackEnabled: data.slackEnabled ?? false,
          slackWebhook: data.slackWebhook ?? null,
          pushEnabled: data.pushEnabled ?? false,
          mobileEnabled: data.mobileEnabled ?? false,
        });
      }
    } catch {
      // Fallback to defaults if API is unavailable
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const toggleOption = (field: keyof NotificationState) => {
    setSettings((prev) => ({
      ...prev,
      [field]: !prev[field],
    }));
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const response = await apiClient.put(
        Endpoints.SETTINGS.NOTIFICATIONS,
        settings
      );
      const data = response.data?.data;
      if (data) {
        setSettings({
          emailEnabled: data.emailEnabled,
          slackEnabled: data.slackEnabled,
          slackWebhook: data.slackWebhook,
          pushEnabled: data.pushEnabled,
          mobileEnabled: data.mobileEnabled,
        });
      }
      toast.success("Bildirim tercihleri kaydedildi!");
    } catch (err: any) {
      const message =
        err.response?.data?.message || "Ayarlar kaydedilirken bir hata oluştu.";
      toast.error(message);
    } finally {
      setIsSaving(false);
    }
  };

  const options: NotificationOption[] = [
    {
      id: "email",
      title: "E-posta Bildirimleri",
      description:
        "Kritik alarmları ve özet raporları e-posta adresinize gönderir.",
      icon: <Mail size={20} className="text-blue-500" />,
      field: "emailEnabled",
      enabled: settings.emailEnabled,
    },
    {
      id: "slack",
      title: "Slack Entegrasyonu",
      description:
        "Belirlenen kanallara anlık hata ve uyarı mesajları iletir.",
      icon: <Hash size={20} className="text-purple-500" />,
      field: "slackEnabled",
      enabled: settings.slackEnabled,
    },
    {
      id: "browser",
      title: "Tarayıcı (Push) Bildirimleri",
      description:
        "Sayfa açık olmasa bile tarayıcı üzerinden uyarı alın.",
      icon: <BellRing size={20} className="text-accent" />,
      field: "pushEnabled",
      enabled: settings.pushEnabled,
    },
    {
      id: "mobile",
      title: "Mobil Bildirimler",
      description: "Mobil uygulamaya push notification gönderir.",
      icon: <Smartphone size={20} className="text-green-500" />,
      field: "mobileEnabled",
      enabled: settings.mobileEnabled,
    },
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-text-primary">
            Bildirim Tercihleri
          </h2>
          <p className="text-sm text-text-muted mt-1">
            Alarmları ve uyarıları hangi kanallardan alacağınızı seçin.
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

      <div className="space-y-4">
        {isLoading
          ? Array.from({ length: 4 }, (_, i) => (
              <div
                key={i}
                className="h-20 bg-bg-tertiary/30 rounded-2xl border border-border-default animate-pulse"
              />
            ))
          : options.map((opt) => (
              <div
                key={opt.id}
                className="flex items-center justify-between p-5 rounded-2xl border border-border-default bg-bg-tertiary/30 hover:bg-bg-tertiary/50 transition-colors"
              >
                <div className="flex items-start gap-4">
                  <div className="p-3 bg-bg-primary rounded-xl shadow-sm border border-border-default">
                    {opt.icon}
                  </div>
                  <div>
                    <h3 className="font-medium text-text-primary">
                      {opt.title}
                    </h3>
                    <p className="text-sm text-text-muted mt-0.5">
                      {opt.description}
                    </p>
                  </div>
                </div>

                {/* Custom Toggle Switch */}
                <button
                  onClick={() => toggleOption(opt.field)}
                  className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                    opt.enabled
                      ? "bg-accent"
                      : "bg-bg-secondary border-border-default"
                  }`}
                  role="switch"
                  aria-checked={opt.enabled}
                >
                  <span
                    aria-hidden="true"
                    className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                      opt.enabled ? "translate-x-5" : "translate-x-0"
                    }`}
                  />
                </button>
              </div>
            ))}
      </div>
    </div>
  );
}
