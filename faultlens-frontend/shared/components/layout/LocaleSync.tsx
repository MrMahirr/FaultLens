"use client";

import { useEffect } from "react";
import { useLocale } from "next-intl";
import { isLocale } from "@/i18n/config";
import { hasExplicitLocale, useChangeLocale } from "@/shared/lib/locale";
import { useSystemSettingsQuery } from "@/features/settings/api/useSystemSettings";

/**
 * Applies the instance default language (system_settings.language) until the user picks one
 * in the language menu; the user's choice always wins.
 */
export function LocaleSync() {
  const locale = useLocale();
  const { change } = useChangeLocale();
  const { data } = useSystemSettingsQuery();

  useEffect(() => {
    const target = data?.language;
    if (isLocale(target) && target !== locale && !hasExplicitLocale()) {
      change(target, { explicit: false });
    }
  }, [data?.language, locale, change]);

  return null;
}
