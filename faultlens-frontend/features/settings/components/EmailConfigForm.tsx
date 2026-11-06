"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Button } from "@/shared/components/ui/Button";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { EmailStatus } from "../hooks/useNotificationSettings";

/**
 * Alarm e-mails are sent by the server over SMTP to the profile address (P07). Nothing to
 * configure per user except that address; the test goes to it as well.
 */
export function EmailConfigForm({ email, emailAvailable }: EmailStatus) {
  const t = useTranslations("settings.notifications.email");

  const handleTest = async () => {
    try {
      await apiClient.post(Endpoints.SETTINGS.TEST.EMAIL);
      toast.success(t("testSent"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("testFailed")));
    }
  };

  if (!emailAvailable) {
    return (
      <p className="banner c-warn small">
        <span>
          {t.rich("noSmtp", { code: (chunks) => <code className="mono">{chunks}</code> })}
        </span>
      </p>
    );
  }

  return (
    <div className="between">
      <p className="small t2">
        {email
          ? t.rich("target", {
              email,
              b: (chunks) => <b className="text-text-primary">{chunks}</b>,
            })
          : t.rich("noAddress", {
              link: (chunks) => (
                <Link href="/settings?tab=account" className="link">
                  {chunks}
                </Link>
              ),
            })}
      </p>
      <Button variant="secondary" size="sm" onClick={handleTest} disabled={!email}>
        {t("test")}
      </Button>
    </div>
  );
}
