import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";

interface SlackConfigFormProps {
  webhookUrl: string | null;
  onChange: (value: string) => void;
}

export function SlackConfigForm({ webhookUrl, onChange }: SlackConfigFormProps) {
  const handleTest = async () => {
    if (!webhookUrl) {
      toast.error("Lütfen önce bir Webhook URL girin.");
      return;
    }
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.SLACK, { webhookUrl });
      toast.success("Test mesajı başarıyla Slack kanalına gönderildi!");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Test mesajı gönderilemedi.");
    }
  };

  return (
    <>
      <div className="flex flex-col gap-1">
        <label className="text-sm font-medium text-text-secondary">Slack Webhook URL</label>
        <input
          type="url"
          placeholder="Slack webhook URL"
          className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-accent"
          value={webhookUrl || ""}
          onChange={(e) => onChange(e.target.value)}
        />
      </div>
      <div className="flex justify-end mt-2">
        <Button variant="outline" size="sm" onClick={handleTest}>
          Test Mesajı Gönder
        </Button>
      </div>
    </>
  );
}
