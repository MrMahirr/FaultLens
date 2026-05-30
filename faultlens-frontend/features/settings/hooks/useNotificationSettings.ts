import { useState, useEffect, useCallback } from "react";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";

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
          emailjsServiceId: data.emailjsServiceId ?? null,
          emailjsTemplateId: data.emailjsTemplateId ?? null,
          emailjsPublicKey: data.emailjsPublicKey ?? null,
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
    } catch (err: any) {
      toast.error("Ayarlar kaydedilemedi.");
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
    } catch (err: any) {
      const message =
        err.response?.data?.message || "Ayarlar kaydedilirken bir hata oluştu.";
      toast.error(message);
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
