"use client";

import { useLocale, useTranslations } from "next-intl";
import { Moon, Sun, Monitor } from "lucide-react";
import { useUIStore } from "@/shared/store/ui.store";
import { useTheme } from "@/shared/hooks/useTheme";
import { useChangeLocale } from "@/shared/lib/locale";
import { LOCALES, type Locale } from "@/i18n/config";
import { Switch } from "@/shared/components/ui/Switch";
import type { ThemePreference } from "@/shared/lib/theme";

const THEMES: { value: ThemePreference; icon: React.ReactNode }[] = [
  { value: "system", icon: <Monitor size={14} /> },
  { value: "dark", icon: <Moon size={14} /> },
  { value: "light", icon: <Sun size={14} /> },
];

/** One labelled settings row: text on the left, control on the right. */
function Row({
  id,
  label,
  hint,
  children,
}: {
  id: string;
  label: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="between py-4 border-b border-border-default last:border-b-0">
      <div className="min-w-0 max-w-xl">
        <p id={id} className="small font-medium">
          {label}
        </p>
        {hint && (
          <p id={`${id}-hint`} className="xs t3 mt-0.5">
            {hint}
          </p>
        )}
      </div>
      {children}
    </div>
  );
}

export function AppearanceSettings() {
  const t = useTranslations("settings.appearance");
  const tShell = useTranslations("shell");
  const { theme, resolvedTheme, setTheme } = useTheme();
  const locale = useLocale();
  const { change: changeLocale, pending } = useChangeLocale();
  const motion = useUIStore((state) => state.motion);
  const setMotion = useUIStore((state) => state.setMotion);
  const compactMode = useUIStore((state) => state.compactMode);
  const setCompactMode = useUIStore((state) => state.setCompactMode);

  return (
    <div className="stack gap-4">
      <header>
        <h2 className="h2">{t("title")}</h2>
        <p className="small t3 mt-1">{t("subtitle")}</p>
      </header>

      <div className="card px-4">
        <Row
          id="pref-theme"
          label={t("theme")}
          hint={
            theme === "system"
              ? t("systemResolved", { theme: t(`themes.${resolvedTheme}`) })
              : undefined
          }
        >
          <div className="seg" role="radiogroup" aria-labelledby="pref-theme">
            {THEMES.map(({ value, icon }) => (
              <button
                key={value}
                type="button"
                role="radio"
                aria-checked={theme === value}
                onClick={() => setTheme(value)}
                className="inline-flex items-center gap-1.5"
              >
                <span aria-hidden="true" className="inline-flex">
                  {icon}
                </span>
                {t(`themes.${value}`)}
              </button>
            ))}
          </div>
        </Row>

        <Row id="pref-language" label={t("language")} hint={t("languageHint")}>
          <div
            className="seg"
            role="radiogroup"
            aria-labelledby="pref-language"
            aria-busy={pending || undefined}
          >
            {LOCALES.map((l) => (
              <button
                key={l}
                type="button"
                role="radio"
                aria-checked={locale === l}
                lang={l}
                onClick={() => changeLocale(l as Locale)}
              >
                {tShell(`languages.${l}`)}
              </button>
            ))}
          </div>
        </Row>

        <Row id="pref-motion" label={t("motion")} hint={t("motionHint")}>
          <Switch
            checked={motion === "on"}
            onCheckedChange={(on) => setMotion(on ? "on" : "off")}
            aria-labelledby="pref-motion"
            aria-describedby="pref-motion-hint"
          />
        </Row>

        <Row id="pref-compact" label={t("compact")} hint={t("compactHint")}>
          <Switch
            checked={compactMode}
            onCheckedChange={setCompactMode}
            aria-labelledby="pref-compact"
            aria-describedby="pref-compact-hint"
          />
        </Row>
      </div>
    </div>
  );
}
