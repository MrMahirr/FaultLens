import Link from "next/link";
import { getTranslations } from "next-intl/server";

export default async function NotFound() {
  const t = await getTranslations("errors");
  return (
    <main className="min-h-screen grid place-items-center p-6">
      <div className="empty max-w-md">
        <p className="mono xs t3">404</p>
        <h1 className="h1 text-text-primary">{t("notFoundTitle")}</h1>
        <p className="small">{t("notFoundDescription")}</p>
        <Link href="/dashboard" className="btn mt-3">
          {t("backToDashboard")}
        </Link>
      </div>
    </main>
  );
}
