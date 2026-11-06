"use client";

import { useLocale, useTranslations } from "next-intl";
import { Languages, Monitor, Moon, Sun } from "lucide-react";
import { Dropdown } from "@/shared/components/ui/Dropdown";
import { useTheme } from "@/shared/hooks/useTheme";
import { useChangeLocale } from "@/shared/lib/locale";
import { LOCALES, type Locale } from "@/i18n/config";
import type { ThemePreference } from "@/shared/lib/theme";

/** Language switcher (tr/en); a pick here is the user's explicit choice. */
export function LanguageMenu() {
  const t = useTranslations("shell");
  const locale = useLocale();
  const { change } = useChangeLocale();

  return (
    <Dropdown
      label={t("language")}
      trigger={
        <button type="button" className="btn btn-ghost btn-sm gap-1.5" aria-label={t("language")}>
          <Languages size={15} aria-hidden="true" />
          <span className="mono xs uppercase">{locale}</span>
        </button>
      }
      items={LOCALES.map((l) => ({ value: l, label: t(`languages.${l}`), checked: l === locale }))}
      onSelect={(value) => change(value as Locale)}
    />
  );
}

/** System / dark / light; "System" shows what it currently resolves to. */
export function ThemeMenu() {
  const t = useTranslations("shell");
  const { theme, resolvedTheme, setTheme } = useTheme();
  const icon =
    theme === "system" ? (
      <Monitor size={16} />
    ) : resolvedTheme === "dark" ? (
      <Moon size={16} />
    ) : (
      <Sun size={16} />
    );

  return (
    <Dropdown
      label={t("theme")}
      trigger={
        <button type="button" className="btn btn-ghost btn-sm btn-icon" aria-label={t("theme")}>
          <span aria-hidden="true" className="inline-flex">
            {icon}
          </span>
        </button>
      }
      items={(["system", "dark", "light"] as ThemePreference[]).map((value) => ({
        value,
        label: t(`themes.${value}`),
        checked: theme === value,
        hint: value === "system" ? t(`themes.${resolvedTheme}`) : undefined,
      }))}
      onSelect={(value) => setTheme(value as ThemePreference)}
    />
  );
}
