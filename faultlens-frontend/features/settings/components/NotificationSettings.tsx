"use client";

import { Mail, Hash, BellRing, Smartphone } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { useNotificationSettings } from "../hooks/useNotificationSettings";
import { NotificationToggleCard } from "./NotificationToggleCard";
import { EmailConfigForm } from "./EmailConfigForm";
import { SlackConfigForm } from "./SlackConfigForm";

export function NotificationSettings() {
  const { settings, setSettings, isLoading, isSaving, toggleOption, handleSave } = useNotificationSettings();

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
        {isLoading ? (
          Array.from({ length: 4 }, (_, i) => (
            <div
              key={i}
              className="h-20 bg-bg-tertiary/30 rounded-2xl border border-border-default animate-pulse"
            />
          ))
        ) : (
          <>
            <NotificationToggleCard
              id="email"
              title="E-posta Bildirimleri"
              description="Kritik alarmları ve özet raporları e-posta adresinize gönderir."
              icon={<Mail size={20} className="text-blue-500" />}
              enabled={settings.emailEnabled}
              onToggle={() => toggleOption("emailEnabled")}
            >
              <EmailConfigForm
                serviceId={settings.emailjsServiceId ?? null}
                templateId={settings.emailjsTemplateId ?? null}
                publicKey={settings.emailjsPublicKey ?? null}
                onChange={(field, value) => setSettings({ ...settings, [field]: value })}
              />
            </NotificationToggleCard>

            <NotificationToggleCard
              id="slack"
              title="Slack Entegrasyonu"
              description="Belirlenen kanallara anlık hata ve uyarı mesajları iletir."
              icon={<Hash size={20} className="text-purple-500" />}
              enabled={settings.slackEnabled}
              onToggle={() => toggleOption("slackEnabled")}
            >
              <SlackConfigForm
                webhookUrl={settings.slackWebhook}
                onChange={(value) => setSettings({ ...settings, slackWebhook: value })}
              />
            </NotificationToggleCard>

            <NotificationToggleCard
              id="browser"
              title="Tarayıcı (Push) Bildirimleri"
              description="Sayfa açık olmasa bile tarayıcı üzerinden uyarı alın."
              icon={<BellRing size={20} className="text-accent" />}
              enabled={settings.pushEnabled}
              onToggle={() => toggleOption("pushEnabled")}
            >
              <div className="flex items-center justify-between">
                <p className="text-sm text-text-secondary">Uygulama içi anlık bildirimleri test edin.</p>
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={async () => {
                    try {
                      await apiClient.post(Endpoints.SETTINGS.TEST.PUSH);
                      toast.success("Push bildirimi tetiklendi.");
                    } catch (err: any) {
                      toast.error(err.response?.data?.message || "Bildirim tetiklenemedi.");
                    }
                  }}
                >
                  Push Bildirimi Tetikle
                </Button>
              </div>
            </NotificationToggleCard>

            <NotificationToggleCard
              id="mobile"
              title="Mobil Bildirimler"
              description="Mobil uygulamaya push notification gönderir."
              icon={<Smartphone size={20} className="text-green-500" />}
              enabled={settings.mobileEnabled}
              onToggle={() => toggleOption("mobileEnabled")}
            />
          </>
        )}
      </div>
    </div>
  );
}
