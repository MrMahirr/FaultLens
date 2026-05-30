"use client";

import { useState } from "react";
import { Mail, Hash, BellRing, Smartphone } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";

interface NotificationOption {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  enabled: boolean;
}

export function NotificationSettings() {
  const [options, setOptions] = useState<NotificationOption[]>([
    {
      id: "email",
      title: "E-posta Bildirimleri",
      description: "Kritik alarmları ve özet raporları e-posta adresinize gönderir.",
      icon: <Mail size={20} className="text-blue-500" />,
      enabled: true,
    },
    {
      id: "slack",
      title: "Slack Entegrasyonu",
      description: "Belirlenen kanallara anlık hata ve uyarı mesajları iletir.",
      icon: <Hash size={20} className="text-purple-500" />,
      enabled: false,
    },
    {
      id: "browser",
      title: "Tarayıcı (Push) Bildirimleri",
      description: "Sayfa açık olmasa bile tarayıcı üzerinden uyarı alın.",
      icon: <BellRing size={20} className="text-accent" />,
      enabled: true,
    },
    {
      id: "mobile",
      title: "Mobil Bildirimler",
      description: "Mobil uygulamaya push notification gönderir.",
      icon: <Smartphone size={20} className="text-green-500" />,
      enabled: false,
    },
  ]);

  const toggleOption = (id: string) => {
    setOptions((prev) =>
      prev.map((opt) => (opt.id === id ? { ...opt, enabled: !opt.enabled } : opt))
    );
  };

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
        <Button variant="primary" size="sm">
          Değişiklikleri Kaydet
        </Button>
      </div>

      <div className="space-y-4">
        {options.map((opt) => (
          <div
            key={opt.id}
            className="flex items-center justify-between p-5 rounded-2xl border border-border-default bg-bg-tertiary/30 hover:bg-bg-tertiary/50 transition-colors"
          >
            <div className="flex items-start gap-4">
              <div className="p-3 bg-bg-primary rounded-xl shadow-sm border border-border-default">
                {opt.icon}
              </div>
              <div>
                <h3 className="font-medium text-text-primary">{opt.title}</h3>
                <p className="text-sm text-text-muted mt-0.5">
                  {opt.description}
                </p>
              </div>
            </div>

            {/* Custom Toggle Switch */}
            <button
              onClick={() => toggleOption(opt.id)}
              className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                opt.enabled ? "bg-accent" : "bg-bg-secondary border-border-default"
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
