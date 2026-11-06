"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { getApiErrorMessage } from "@/shared/api/errors";

interface SlackConfigFormProps {
  webhookUrl: string | null;
  onChange: (value: string) => void;
}

export function SlackConfigForm({ webhookUrl, onChange }: SlackConfigFormProps) {
  const t = useTranslations("settings.notifications");

  const handleTest = async () => {
    if (!webhookUrl) {
      toast.error(t("urlRequired"));
      return;
    }
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.SLACK, { webhookUrl });
      toast.success(t("slack.testSent"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("slack.testFailed")));
    }
  };

  return (
    <>
      <Input
        id="slack-webhook-url"
        label={t("slack.url")}
        type="url"
        placeholder="https://hooks.slack.com/services/…"
        className="mono"
        hint={t("slack.urlHint")}
        autoComplete="off"
        value={webhookUrl || ""}
        onChange={(e) => onChange(e.target.value)}
      />
      <div className="flex justify-end">
        <Button variant="secondary" size="sm" onClick={handleTest}>
          {t("slack.test")}
        </Button>
      </div>
    </>
  );
}
