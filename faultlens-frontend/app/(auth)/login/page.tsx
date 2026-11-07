"use client";

import { useTranslations } from "next-intl";
import { LoginForm } from "@/features/auth/components/LoginForm";

export default function LoginPage() {
  const t = useTranslations("auth.login");
  return (
    <div className="stack gap-6">
      <div>
        <h1 className="h1">{t("title")}</h1>
        <p className="small t3 mt-1.5">{t("subtitle")}</p>
      </div>
      <LoginForm />
    </div>
  );
}
