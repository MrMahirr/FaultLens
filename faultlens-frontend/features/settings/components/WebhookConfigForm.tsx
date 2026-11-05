import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";

interface WebhookConfigFormProps {
  webhookUrl: string | null;
  onChange: (value: string) => void;
}

/** Generic webhook: alarms are POSTed as JSON to a public https URL. */
export function WebhookConfigForm({ webhookUrl, onChange }: WebhookConfigFormProps) {
  const handleTest = async () => {
    if (!webhookUrl) {
      toast.error("Lütfen önce bir Webhook URL girin.");
      return;
    }
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.WEBHOOK, { webhookUrl });
      toast.success("Webhook test isteği gönderildi!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Webhook isteği başarısız oldu."));
    }
  };

  return (
    <>
      <div className="flex flex-col gap-1">
        <label htmlFor="webhook-url" className="text-sm font-medium text-text-secondary">
          Webhook URL
        </label>
        <input
          id="webhook-url"
          type="url"
          placeholder="https://ornek.com/faultlens-hook"
          className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-accent"
          value={webhookUrl || ""}
          onChange={(e) => onChange(e.target.value)}
        />
        <p className="text-xs text-text-muted">
          Herkese açık bir sunucuda <code>https://</code> adresi olmalı. Gövde:{" "}
          <code>{'{"event":"alarm","title","message","severity","at","url"}'}</code>
        </p>
      </div>
      <div className="flex justify-end mt-2">
        <Button variant="secondary" size="sm" onClick={handleTest}>
          Test İsteği Gönder
        </Button>
      </div>
    </>
  );
}
