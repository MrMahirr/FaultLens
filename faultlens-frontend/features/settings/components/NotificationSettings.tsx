"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Mail, Hash, BellRing, Smartphone, Webhook } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { getApiErrorMessage } from "@/shared/api/errors";
import { useNotificationSettings } from "../hooks/useNotificationSettings";
import { NotificationToggleCard } from "./NotificationToggleCard";
import { EmailConfigForm } from "./EmailConfigForm";
import { SlackConfigForm } from "./SlackConfigForm";
import { WebhookConfigForm } from "./WebhookConfigForm";

export function NotificationSettings() {
  const t = useTranslations("settings.notifications");
  const tSettings = useTranslations("settings");
  const { settings, setSettings, emailStatus, isLoading, isSaving, toggleOption, handleSave } =
    useNotificationSettings();

  const triggerPush = async () => {
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.PUSH);
      toast.success(t("push.testSent"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("push.testFailed")));
    }
  };

  return (
    <div className="stack gap-4">
      <header className="between">
        <div>
          <h2 className="h2">{t("title")}</h2>
          <p className="small t3 mt-1">{t("subtitle")}</p>
        </div>
        <Button variant="primary" size="sm" onClick={handleSave} loading={isSaving}>
          {tSettings("saveChanges")}
        </Button>
      </header>

      {isLoading ? (
        <div className="stack" aria-hidden="true">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="skel h-[72px]" />
          ))}
        </div>
      ) : (
        <div className="stack">
          <NotificationToggleCard
            id="email"
            title={t("email.title")}
            description={t("email.description")}
            icon={<Mail size={16} />}
            enabled={settings.emailEnabled}
            onToggle={() => toggleOption("emailEnabled")}
          >
            <EmailConfigForm {...emailStatus} />
          </NotificationToggleCard>

          <NotificationToggleCard
            id="slack"
            title={t("slack.title")}
            description={t("slack.description")}
            icon={<Hash size={16} />}
            enabled={settings.slackEnabled}
            onToggle={() => toggleOption("slackEnabled")}
          >
            <SlackConfigForm
              webhookUrl={settings.slackWebhook}
              onChange={(value) => setSettings({ ...settings, slackWebhook: value })}
            />
          </NotificationToggleCard>

          <NotificationToggleCard
            id="webhook"
            title={t("webhook.title")}
            description={t("webhook.description")}
            icon={<Webhook size={16} />}
            enabled={settings.webhookEnabled}
            onToggle={() => toggleOption("webhookEnabled")}
          >
            <WebhookConfigForm
              webhookUrl={settings.webhookUrl}
              onChange={(value) => setSettings({ ...settings, webhookUrl: value })}
            />
          </NotificationToggleCard>

          <NotificationToggleCard
            id="browser"
            title={t("push.title")}
            description={t("push.description")}
            icon={<BellRing size={16} />}
            enabled={settings.pushEnabled}
            onToggle={() => toggleOption("pushEnabled")}
          >
            <div className="between">
              <p className="small t2">{t("push.testHint")}</p>
              <Button variant="secondary" size="sm" onClick={triggerPush}>
                {t("push.test")}
              </Button>
            </div>
          </NotificationToggleCard>

          <NotificationToggleCard
            id="mobile"
            title={t("mobile.title")}
            description={t("mobile.description")}
            icon={<Smartphone size={16} />}
            enabled={settings.mobileEnabled}
            onToggle={() => toggleOption("mobileEnabled")}
          />
        </div>
      )}
    </div>
  );
}
