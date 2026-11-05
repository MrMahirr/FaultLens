import Link from "next/link";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { EmailStatus } from "../hooks/useNotificationSettings";

/**
 * Alarm e-mails are sent by the server over SMTP to the profile address (P07). Nothing to
 * configure per user except that address; the test goes to it as well.
 */
export function EmailConfigForm({ email, emailAvailable }: EmailStatus) {
  const handleTest = async () => {
    try {
      const response = await apiClient.post(Endpoints.SETTINGS.TEST.EMAIL);
      toast.success(response.data?.data ?? "Test e-postası gönderildi.");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "E-posta gönderilemedi."));
    }
  };

  if (!emailAvailable) {
    return (
      <p className="text-xs text-text-muted mt-2 border-l-2 border-warning pl-2">
        Sunucuda SMTP yapılandırılmamış; e-posta bildirimleri gönderilmez. Yöneticinizden{" "}
        <code>SMTP_HOST</code> ve ilgili ayarları tanımlamasını isteyin.
      </p>
    );
  }

  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <p className="text-sm text-text-secondary">
        {email ? (
          <>
            Alarmlar <b className="text-text-primary">{email}</b> adresine gönderilir.
          </>
        ) : (
          <>
            Profilinizde e-posta adresi yok.{" "}
            <Link href="/settings?tab=account" className="text-accent hover:underline">
              Hesap ayarlarından ekleyin
            </Link>
            .
          </>
        )}
      </p>
      <Button variant="secondary" size="sm" onClick={handleTest} disabled={!email}>
        Test E-postası Gönder
      </Button>
    </div>
  );
}
