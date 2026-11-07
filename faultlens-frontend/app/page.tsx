import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { LandingPage } from "@/features/landing/components/LandingPage";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("landing.meta");
  return { title: t("title") };
}

/** Public landing page. proxy.ts sends visitors with a live session on to /dashboard. */
export default function RootPage() {
  return <LandingPage />;
}
