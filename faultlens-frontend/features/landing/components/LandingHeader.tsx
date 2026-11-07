"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { LanguageMenu, ThemeMenu } from "@/shared/components/layout/PreferenceMenus";
import { BrandMark } from "@/shared/components/layout/BrandMark";

const SECTIONS = ["features", "how", "security"] as const;

/**
 * Sticky, translucent top bar. Its hairline only appears once the page has scrolled, so the
 * hero opens clean; section links collapse below md, sign-in below sm.
 */
export function LandingHeader() {
  const t = useTranslations("landing.nav");
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className="sticky top-0 z-30 border-b backdrop-blur-[10px] transition-[border-color] duration-[var(--t-2)]"
      style={{
        background: "color-mix(in srgb, var(--bg) 82%, transparent)",
        borderColor: scrolled ? "var(--line)" : "transparent",
      }}
    >
      <div className="row between flex-nowrap max-w-[1160px] mx-auto px-4 sm:px-6 h-[60px]">
        <Link href="/" className="brand p-0">
          <BrandMark />
          FaultLens
        </Link>

        <nav className="hidden md:flex row small gap-6" aria-label={t("label")}>
          {SECTIONS.map((id) => (
            <a key={id} href={`#${id}`} className="t3 link">
              {t(id)}
            </a>
          ))}
        </nav>

        <div className="row flex-nowrap gap-1.5">
          <LanguageMenu />
          <ThemeMenu />
          <Link href="/login" className="btn btn-sm btn-ghost hidden sm:inline-flex">
            {t("login")}
          </Link>
          <Link href="/login" className="btn btn-sm btn-primary">
            {t("start")}
          </Link>
        </div>
      </div>
    </header>
  );
}
