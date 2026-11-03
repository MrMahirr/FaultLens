import { useState, useEffect } from "react";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";

export interface NotificationState {
  emailEnabled: boolean;
  slackEnabled: boolean;
  slackWebhook: string | null;
  pushEnabled: boolean;
  mobileEnabled: boolean;
  emailjsServiceId?: string | null;
  emailjsTemplateId?: string | null;
  emailjsPublicKey?: string | null;
}

const DEFAULT_STATE: NotificationState = {
  emailEnabled: true,
  slackEnabled: false,
  slackWebhook: null,
  pushEnabled: false,
  mobileEnabled: false,
  emailjsServiceId: null,
  emailjsTemplateId: null,
  emailjsPublicKey: null,
};

export function useNotificationSettings() {
  const [settings, setSettings] = useState<NotificationState>(DEFAULT_STATE);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Load once on mount. State is only set in the promise callbacks, and not at
  // all after unmount.
  useEffect(() => {
    let active = true;
    apiClient
      .get(Endpoints.SETTINGS.NOTIFICATIONS)
      .then((response) => {
        const data = response.data?.data;
        if (active && data) {
          setSettings({
            emailEnabled: data.emailEnabled ?? true,
            slackEnabled: data.slackEnabled ?? false,
            slackWebhook: data.slackWebhook ?? null,
            pushEnabled: data.pushEnabled ?? false,
            mobileEnabled: data.mobileEnabled ?? false,
            emailjsServiceId: data.emailjsServiceId ?? null,
            emailjsTemplateId: data.emailjsTemplateId ?? null,
            emailjsPublicKey: data.emailjsPublicKey ?? null,
          });
        }
      })
      .catch(() => {
        // Fallback to defaults if API is unavailable
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const toggleOption = async (field: "emailEnabled" | "slackEnabled" | "pushEnabled" | "mobileEnabled") => {
    const previousSettings = { ...settings };
    const newSettings = {
      ...settings,
      [field]: !settings[field],
    };
    setSettings(newSettings);

    try {
      const response = await apiClient.put(
        Endpoints.SETTINGS.NOTIFICATIONS,
        newSettings
      );
      const data = response.data?.data;
      if (data) {
        setSettings({
          emailEnabled: data.emailEnabled,
          slackEnabled: data.slackEnabled,
          slackWebhook: data.slackWebhook,
          pushEnabled: data.pushEnabled,
          mobileEnabled: data.mobileEnabled,
          emailjsServiceId: data.emailjsServiceId,
          emailjsTemplateId: data.emailjsTemplateId,
          emailjsPublicKey: data.emailjsPublicKey,
        });
      }
      toast.success("Bildirim tercihi güncellendi!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Ayarlar kaydedilemedi."));
      setSettings(previousSettings);
    }
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
          emailjsServiceId: data.emailjsServiceId,
          emailjsTemplateId: data.emailjsTemplateId,
          emailjsPublicKey: data.emailjsPublicKey,
        });
      }
      toast.success("Bildirim tercihleri kaydedildi!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Ayarlar kaydedilirken bir hata oluştu."));
    } finally {
      setIsSaving(false);
    }
  };

  return {
    settings,
    setSettings,
    isLoading,
    isSaving,
    toggleOption,
    handleSave,
  };
}
