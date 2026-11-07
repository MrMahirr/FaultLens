import Link from "next/link";
import { useTranslations } from "next-intl";
import {
  ArrowRight,
  ArrowUp,
  BellRing,
  GitBranch,
  ListTree,
  Send,
  ShieldCheck,
  Sparkles,
  type LucideIcon,
} from "lucide-react";
import { HeroPreview } from "./HeroPreview";
import { LandingHeader } from "./LandingHeader";
import { Reveal } from "./Reveal";

const SOURCES = ["KUBERNETES", "DOCKER", "SSH", "CI/CD", "LOCAL_FILE", "WEBSOCKET · /ws/logs"];

const FEATURES: {
  key: "grouping" | "deploy" | "rca" | "alerts" | "notify" | "rbac";
  icon: LucideIcon;
  color: string;
}[] = [
  { key: "grouping", icon: ListTree, color: "var(--fg)" },
  { key: "deploy", icon: GitBranch, color: "var(--signal-ink)" },
  { key: "rca", icon: Sparkles, color: "var(--ai)" },
  { key: "alerts", icon: BellRing, color: "var(--crit)" },
  { key: "notify", icon: Send, color: "var(--info)" },
  { key: "rbac", icon: ShieldCheck, color: "var(--ok)" },
];

const STEPS = [
  { key: "connect", code: "kubectl -n prod logs -f" },
  { key: "watch", code: "GET /api/v1/logs/groups" },
  { key: "analyze", code: "POST /api/v1/analyses/trigger" },
] as const;

const SECURITY = ["sessions", "secrets", "rbac", "retention"] as const;

/** Stagger between siblings revealed together (ms). */
const STAGGER = 70;

/**
 * Public landing page (Instrument "Landing" board): hero with a live incident mock, sources,
 * features, three setup steps, security and a closing call to action.
 */
export function LandingPage() {
  const t = useTranslations("landing");

  return (
    <div id="top" className="min-h-screen" style={{ background: "var(--bg)", color: "var(--fg)" }}>
      <a
        href="#main"
        className="btn btn-sm sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-3 focus:z-50"
      >
        {t("nav.skip")}
      </a>

      <LandingHeader />

      <main id="main">
        {/* Hero: CSS entrance only (.in + .d1–d4), already gated by the motion setting. */}
        <section className="max-w-[1160px] mx-auto px-4 sm:px-6 pt-16 sm:pt-24 pb-16 sm:pb-[72px] flex flex-wrap gap-14 items-center">
          <div className="stack flex-[1_1_440px] gap-[22px] min-w-0">
            <span className="eyebrow in">{t("hero.eyebrow")}</span>
            <h1 className="display in d1 m-0">
              {t("hero.titleA")} <span className="mark">{t("hero.titleB")}</span> {t("hero.titleC")}
            </h1>
            <p className="lead t2 in d2 m-0 max-w-[52ch]">{t("hero.lead")}</p>
            <div className="row in d3 gap-2.5">
              <Link href="/login" className="btn btn-primary group">
                {t("hero.cta")}
                <ArrowRight
                  size={15}
                  aria-hidden="true"
                  className="transition-transform duration-[var(--t-2)] group-hover:translate-x-0.5"
                />
              </Link>
              <a href="#how" className="btn">
                {t("hero.cta2")}
              </a>
            </div>
            <div className="row xs t3 in d4 gap-[18px]">
              <span className="row gap-1.5">
                <span className="dot live" style={{ "--c": "var(--ok)" } as React.CSSProperties} />
                {t("hero.metaLive")}
              </span>
              <span>{t("hero.metaSetup")}</span>
              <span>{t("hero.metaHost")}</span>
            </div>
          </div>

          <HeroPreview />
        </section>

        <section aria-label={t("sources.label")} className="border-y border-[var(--line)]">
          <ul className="row mono xs t3 list-none m-0 max-w-[1160px] mx-auto px-4 sm:px-6 py-[18px] justify-between gap-x-6 gap-y-2">
            <li className="t4" aria-hidden="true">
              {t("sources.title")}
            </li>
            {SOURCES.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </section>

        <section
          id="features"
          className="scroll-mt-20 max-w-[1160px] mx-auto px-4 sm:px-6 pt-24 pb-12"
        >
          <Reveal className="stack gap-2.5 max-w-[640px] mb-10">
            <span className="eyebrow">{t("features.eyebrow")}</span>
            <h2 className="h1 m-0">{t("features.title")}</h2>
          </Reveal>
          <div
            className="grid sm:grid-cols-2 lg:grid-cols-3 gap-px overflow-hidden rounded-[14px] border border-[var(--line)]"
            style={{ background: "var(--line)" }}
          >
            {FEATURES.map(({ key, icon: Icon, color }, i) => (
              <Reveal
                key={key}
                delay={(i % 3) * STAGGER}
                className="stack gap-2.5 p-7"
                style={{ background: "var(--surface)" }}
              >
                <Icon size={20} strokeWidth={1.8} style={{ color }} aria-hidden="true" />
                <h3 className="h3 mt-1 mb-0">{t(`features.${key}.title`)}</h3>
                <p className="small t3 m-0">{t(`features.${key}.body`)}</p>
              </Reveal>
            ))}
          </div>
        </section>

        <section id="how" className="scroll-mt-20 max-w-[1160px] mx-auto px-4 sm:px-6 py-[72px]">
          <Reveal className="stack gap-2.5 max-w-[640px] mb-10">
            <span className="eyebrow">{t("how.eyebrow")}</span>
            <h2 className="h1 m-0">{t("how.title")}</h2>
          </Reveal>
          <ol className="grid md:grid-cols-3 gap-4 list-none m-0 p-0">
            {STEPS.map(({ key, code }, i) => (
              <Reveal as="li" key={key} delay={i * STAGGER} className="card pad stack gap-3">
                <span className="mono xs" style={{ color: "var(--signal-ink)" }}>
                  {`0${i + 1}`}
                </span>
                <h3 className="h3 m-0">{t(`how.${key}.title`)}</h3>
                <p className="small t3 m-0">{t(`how.${key}.body`)}</p>
                <code className="code xs mt-auto">{code}</code>
              </Reveal>
            ))}
          </ol>
        </section>

        <section
          id="security"
          className="scroll-mt-20 max-w-[1160px] mx-auto px-4 sm:px-6 py-[72px]"
        >
          <div className="flex flex-wrap gap-12">
            <Reveal className="stack flex-[1_1_320px] gap-3">
              <span className="eyebrow">{t("security.eyebrow")}</span>
              <h2 className="h1 m-0">{t("security.title")}</h2>
              <p className="t2 m-0">{t("security.body")}</p>
            </Reveal>
            <dl className="grid sm:grid-cols-2 flex-[2_1_560px] gap-4 m-0 min-w-0">
              {SECURITY.map((key, i) => (
                <Reveal
                  key={key}
                  delay={(i % 2) * STAGGER}
                  className="stack gap-1.5 pt-4 border-t border-[var(--line)]"
                >
                  <dt className="small font-medium">{t(`security.${key}.title`)}</dt>
                  <dd className="small t3 m-0">{t(`security.${key}.body`)}</dd>
                </Reveal>
              ))}
            </dl>
          </div>
        </section>

        <section className="max-w-[1160px] mx-auto px-4 sm:px-6 pt-12 pb-24">
          <Reveal className="inverted stack gap-[18px] rounded-[14px] p-8 sm:p-14 items-start">
            <h2 className="h1 m-0" style={{ color: "inherit" }}>
              {t("cta.title")}
            </h2>
            <p className="m-0 opacity-70 max-w-[56ch]">{t("cta.body")}</p>
            <Link
              href="/login"
              // Lime fill in both themes, so the label is always the dark --on-signal ink.
              className="btn group border-0 bg-signal text-on-signal hover:brightness-95"
            >
              {t("cta.button")}
              <ArrowRight
                size={15}
                aria-hidden="true"
                className="transition-transform duration-[var(--t-2)] group-hover:translate-x-0.5"
              />
            </Link>
          </Reveal>
        </section>
      </main>

      <footer className="border-t border-[var(--line)]">
        <div className="row between xs t3 max-w-[1160px] mx-auto px-4 sm:px-6 py-7 gap-4">
          <span>{t("footer.copyright", { year: new Date().getFullYear() })}</span>
          <a href="#top" className="link row gap-1">
            <ArrowUp size={12} aria-hidden="true" />
            {t("footer.backToTop")}
          </a>
        </div>
      </footer>
    </div>
  );
}
