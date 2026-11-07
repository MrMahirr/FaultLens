import { cookies } from "next/headers";
import { getRequestConfig } from "next-intl/server";
import { DEFAULT_LOCALE, LOCALE_COOKIE, isLocale } from "./config";
import { MESSAGES } from "@/messages";

/**
 * No locale segment in the URL: the language comes from the NEXT_LOCALE cookie, which the
 * language menu (user choice) or the system default (system_settings.language) sets.
 */
export default getRequestConfig(async () => {
  const stored = (await cookies()).get(LOCALE_COOKIE)?.value;
  const locale = isLocale(stored) ? stored : DEFAULT_LOCALE;
  return { locale, messages: MESSAGES[locale], timeZone: "Europe/Istanbul" };
});
