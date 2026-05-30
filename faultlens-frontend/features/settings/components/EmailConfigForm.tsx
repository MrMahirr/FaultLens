import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";

interface EmailConfigFormProps {
  serviceId: string | null;
  templateId: string | null;
  publicKey: string | null;
  onChange: (field: "emailjsServiceId" | "emailjsTemplateId" | "emailjsPublicKey", value: string) => void;
}

export function EmailConfigForm({ serviceId, templateId, publicKey, onChange }: EmailConfigFormProps) {
  const handleTest = async () => {
    const emailInput = document.getElementById("test-email-input") as HTMLInputElement;
    const testEmail = emailInput?.value;
    if (!testEmail) {
      toast.error("Lütfen bir e-posta adresi girin.");
      return;
    }
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.EMAIL, { emailAddress: testEmail });
      toast.success("Test e-postası başarıyla gönderildi!");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "E-posta gönderilemedi.");
    }
  };

  return (
    <>
      <p className="text-xs text-text-muted mt-2 border-l-2 border-accent pl-2">
        E-posta gönderimleri <b>Email.js</b> kullanılarak yapılmaktadır. Gerekli API anahtarlarını aşağıya girin.
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-text-secondary">Service ID</label>
          <input
            type="text"
            placeholder="service_xxxxx"
            className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-accent"
            value={serviceId || ""}
            onChange={(e) => onChange("emailjsServiceId", e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-text-secondary">Template ID</label>
          <input
            type="text"
            placeholder="template_xxxxx"
            className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-accent"
            value={templateId || ""}
            onChange={(e) => onChange("emailjsTemplateId", e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-1 md:col-span-2">
          <label className="text-sm font-medium text-text-secondary">Public Key (User ID)</label>
          <input
            type="text"
            placeholder="xxxxxxxxxxxxx"
            className="w-full bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-accent"
            value={publicKey || ""}
            onChange={(e) => onChange("emailjsPublicKey", e.target.value)}
          />
        </div>
      </div>
      
      <hr className="border-border-default" />

      <div className="flex flex-col gap-1">
        <label className="text-sm font-medium text-text-secondary">Yapılandırmayı Test Et</label>
        <div className="flex gap-3">
          <input
            type="email"
            id="test-email-input"
            placeholder="ornek@sirket.com"
            className="flex-1 bg-bg-primary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-accent"
          />
          <Button variant="outline" size="sm" onClick={handleTest}>
            Test Gönder
          </Button>
        </div>
      </div>
    </>
  );
}
