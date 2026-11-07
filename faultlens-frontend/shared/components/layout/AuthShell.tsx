"use client";

import { type ReactNode } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Check } from "lucide-react";
import { BrandMark } from "@/shared/components/layout/BrandMark";
import { LanguageMenu, ThemeMenu } from "@/shared/components/layout/PreferenceMenus";

/**
 * Identity pages (Instrument): form on the left (max 380px), brand panel on the right
 * (hidden below lg), language and theme switchers top right; the brand links to the landing.
 */
export function AuthShell({ children }: { children: ReactNode }) {
  const t = useTranslations("auth.login");

  return (
    <div className="min-h-screen bg-bg-primary grid lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <div className="relative flex flex-col">
        <header className="flex items-center justify-between p-4 sm:p-6">
          {/* Back to the public landing page. */}
          <Link href="/" className="brand px-0">
            <BrandMark />
            FaultLens
          </Link>
          <div className="row gap-1">
            <LanguageMenu />
            <ThemeMenu />
          </div>
        </header>

        <main className="flex-1 flex items-center justify-center px-4 pb-16">
          <div className="w-full max-w-[380px] in">{children}</div>
        </main>
      </div>

      <aside
        className="hidden lg:flex flex-col justify-end p-12 border-l border-border-default bg-bg-tertiary relative overflow-hidden"
        aria-hidden="true"
      >
        {/* Hairline grid, the only decoration (Instrument: lines, not shadows). */}
        <div
          className="absolute inset-0 opacity-60"
          style={{
            backgroundImage:
              "linear-gradient(var(--line) 1px, transparent 1px), linear-gradient(90deg, var(--line) 1px, transparent 1px)",
            backgroundSize: "48px 48px",
            maskImage: "linear-gradient(to top, black, transparent 85%)",
          }}
        />
        <div className="relative max-w-md stack gap-4">
          <p className="eyebrow text-accent">FaultLens</p>
          <h2 className="display text-[44px]">{t("brandTitle")}</h2>
          <p className="lead">{t("brandText")}</p>
          <ul className="stack gap-2 m-0 p-0 list-none small t2">
            {(["live", "rca", "deploy"] as const).map((key) => (
              <li key={key} className="row gap-2">
                <Check size={14} className="text-accent" />
                {t(`brandPoints.${key}`)}
              </li>
            ))}
          </ul>
        </div>
      </aside>
    </div>
  );
}
