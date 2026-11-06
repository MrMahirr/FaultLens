"use client";

import { useCallback, useTransition } from "react";
import { useRouter } from "next/navigation";
import { LOCALE_COOKIE, type Locale } from "@/i18n/config";

/** Set when the user picks a language in the menu; then the system default no longer applies. */
const EXPLICIT_KEY = "fl-locale-explicit";
const ONE_YEAR_SEC = 365 * 24 * 60 * 60;

export function writeLocaleCookie(locale: Locale) {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=${ONE_YEAR_SEC}; SameSite=Lax`;
}

export function hasExplicitLocale(): boolean {
  try {
    return localStorage.getItem(EXPLICIT_KEY) === "1";
  } catch {
    return false;
  }
}

/**
 * Switches the UI language: writes the cookie the server reads (i18n/request.ts) and
 * re-renders the server tree with the new messages. `explicit` marks a user choice.
 */
export function useChangeLocale() {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  const change = useCallback(
    (locale: Locale, { explicit = true }: { explicit?: boolean } = {}) => {
      writeLocaleCookie(locale);
      if (explicit) {
        try {
          localStorage.setItem(EXPLICIT_KEY, "1");
        } catch {
          // Private mode: the cookie alone still switches the language.
        }
      }
      document.documentElement.lang = locale;
      startTransition(() => router.refresh());
    },
    [router],
  );

  return { change, pending };
}
