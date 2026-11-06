/** Supported UI languages; matches system_settings.language ('tr' | 'en') in the backend. */
export const LOCALES = ["tr", "en"] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "tr";

/** Cookie holding the active UI language (read on the server, so the first paint is right). */
export const LOCALE_COOKIE = "NEXT_LOCALE";

/** Intl locale tags for dates and numbers. */
export const INTL_LOCALE: Record<Locale, string> = { tr: "tr-TR", en: "en-GB" };

export function isLocale(value: unknown): value is Locale {
  return typeof value === "string" && (LOCALES as readonly string[]).includes(value);
}
