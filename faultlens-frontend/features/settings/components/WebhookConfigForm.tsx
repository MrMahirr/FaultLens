"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { getApiErrorMessage } from "@/shared/api/errors";

interface WebhookConfigFormProps {
  webhookUrl: string | null;
  onChange: (value: string) => void;
}

const BODY_SHAPE = '{"event":"alarm","title","message","severity","at","url"}';

/** Generic webhook: alarms are POSTed as JSON to a public https URL. */
export function WebhookConfigForm({ webhookUrl, onChange }: WebhookConfigFormProps) {
  const t = useTranslations("settings.notifications");

  const handleTest = async () => {
    if (!webhookUrl) {
      toast.error(t("urlRequired"));
      return;
    }
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.WEBHOOK, { webhookUrl });
      toast.success(t("webhook.testSent"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("webhook.testFailed")));
    }
  };

  return (
    <>
      <div className="field">
        <label htmlFor="webhook-url" className="label">
          {t("webhook.url")}
        </label>
        <input
          id="webhook-url"
          type="url"
          placeholder="https://example.com/faultlens-hook"
          className="input mono"
          autoComplete="off"
          aria-describedby="webhook-url-hint"
          value={webhookUrl || ""}
          onChange={(e) => onChange(e.target.value)}
        />
        <p id="webhook-url-hint" className="help">
          {t.rich("webhook.urlHint", {
            body: BODY_SHAPE,
            code: (chunks) => <code className="mono">{chunks}</code>,
          })}
        </p>
      </div>
      <div className="flex justify-end">
        <Button variant="secondary" size="sm" onClick={handleTest}>
          {t("webhook.test")}
        </Button>
      </div>
    </>
  );
}
