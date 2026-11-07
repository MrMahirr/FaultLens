import { useState, useEffect } from "react";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { useTranslations } from "next-intl";
import { getApiErrorMessage } from "@/shared/api/errors";

/** What the user edits (backend UpdateNotificationRequest). */
export interface NotificationState {
  emailEnabled: boolean;
  slackEnabled: boolean;
  slackWebhook: string | null;
  webhookEnabled: boolean;
  webhookUrl: string | null;
  pushEnabled: boolean;
  mobileEnabled: boolean;
}

/** Read-only facts returned alongside the preferences. */
export interface EmailStatus {
  /** Profile address alarm e-mails go to. */
  email: string | null;
  /** Whether the server has SMTP configured (SMTP_HOST). */
  emailAvailable: boolean;
}

export type ToggleField =
  "emailEnabled" | "slackEnabled" | "webhookEnabled" | "pushEnabled" | "mobileEnabled";

const DEFAULT_STATE: NotificationState = {
  emailEnabled: true,
  slackEnabled: false,
  slackWebhook: null,
  webhookEnabled: false,
  webhookUrl: null,
  pushEnabled: false,
  mobileEnabled: false,
};

type NotificationResponse = Partial<NotificationState & EmailStatus>;

function toState(data: NotificationResponse): NotificationState {
  return {
    emailEnabled: data.emailEnabled ?? true,
    slackEnabled: data.slackEnabled ?? false,
    slackWebhook: data.slackWebhook ?? null,
    webhookEnabled: data.webhookEnabled ?? false,
    webhookUrl: data.webhookUrl ?? null,
    pushEnabled: data.pushEnabled ?? false,
    mobileEnabled: data.mobileEnabled ?? false,
  };
}

function toStatus(data: NotificationResponse): EmailStatus {
  return { email: data.email ?? null, emailAvailable: data.emailAvailable ?? false };
}

export function useNotificationSettings() {
  const t = useTranslations("settings.notifications");
  const [settings, setSettings] = useState<NotificationState>(DEFAULT_STATE);
  const [emailStatus, setEmailStatus] = useState<EmailStatus>({
    email: null,
    emailAvailable: false,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const apply = (data: NotificationResponse | undefined) => {
    if (!data) return;
    setSettings(toState(data));
    setEmailStatus(toStatus(data));
  };

  // Load once on mount. State is only set in the promise callbacks, and not at
  // all after unmount.
  useEffect(() => {
    let active = true;
    apiClient
      .get(Endpoints.SETTINGS.NOTIFICATIONS)
      .then((response) => {
        const data: NotificationResponse | undefined = response.data?.data;
        if (active && data) {
          setSettings(toState(data));
          setEmailStatus(toStatus(data));
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

  const toggleOption = async (field: ToggleField) => {
    const previousSettings = { ...settings };
    const newSettings = { ...settings, [field]: !settings[field] };
    setSettings(newSettings);

    try {
      const response = await apiClient.put(Endpoints.SETTINGS.NOTIFICATIONS, newSettings);
      apply(response.data?.data);
      toast.success(t("toggled"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("saveFailed")));
      setSettings(previousSettings);
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const response = await apiClient.put(Endpoints.SETTINGS.NOTIFICATIONS, settings);
      apply(response.data?.data);
      toast.success(t("saved"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("saveFailed")));
    } finally {
      setIsSaving(false);
    }
  };

  return {
    settings,
    setSettings,
    emailStatus,
    isLoading,
    isSaving,
    toggleOption,
    handleSave,
  };
}
