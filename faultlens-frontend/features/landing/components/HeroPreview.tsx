"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Pause, Play, Sparkles } from "lucide-react";
import { useMotionAllowed } from "@/shared/hooks/useMotionAllowed";

/** Error-rate bars of the mocked incident: quiet, then the spike after the deploy. */
const BARS = [
  3, 4, 3, 5, 4, 3, 4, 6, 5, 4, 3, 4, 5, 4, 6, 5, 4, 5, 4, 3, 4, 5, 4, 5, 6, 5, 22, 34, 41, 38, 44,
  40,
];
const PEAK = 44;

const LINES = [
  ["ERROR", "sev-error", "PaymentGatewayTimeoutException: stripe-proxy 3000ms"],
  ["WARN", "sev-warn", "HikariPool-1 - pool at 10/10, waiting 4"],
  ["CRIT", "sev-crit", "Circuit breaker OPEN for stripe-proxy"],
  ["INFO", "sev-info", "GET /api/payments 504 3012ms"],
  ["ERROR", "sev-error", "HikariPool-1 - Connection is not available"],
] as const;

const VISIBLE = 4;
const TICK_MS = 2400;

/**
 * The hero's product mock: an incident panel whose log tail scrolls on its own.
 *
 * The feed only moves while it can be watched and is wanted: motion allowed, tab visible,
 * panel on screen, log tail not under the pointer (so a line can be read), not paused with the button (WCAG 2.2.2). The mock
 * is decorative, hidden from assistive tech; the pause button is not.
 */
export function HeroPreview() {
  const t = useTranslations("landing.hero");
  const motion = useMotionAllowed();
  const ref = useRef<HTMLDivElement>(null);
  const [tick, setTick] = useState(0);
  const [paused, setPaused] = useState(false);
  const [held, setHeld] = useState(false);
  const [onScreen, setOnScreen] = useState(true);
  const [tabVisible, setTabVisible] = useState(true);

  useEffect(() => {
    const el = ref.current;
    if (!el || typeof IntersectionObserver === "undefined") return;
    const io = new IntersectionObserver(([entry]) => setOnScreen(entry.isIntersecting));
    io.observe(el);
    const onVisibility = () => setTabVisible(document.visibilityState === "visible");
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      io.disconnect();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  const running = motion && !paused && !held && onScreen && tabVisible;

  useEffect(() => {
    if (!running) return;
    const id = window.setInterval(() => setTick((n) => n + 1), TICK_MS);
    return () => window.clearInterval(id);
  }, [running]);

  return (
    <div
      ref={ref}
      className="card in d3 flex-[1_1_480px] min-w-0 overflow-hidden"
      style={{ boxShadow: "0 0 0 1px var(--line-2), 0 30px 80px -20px rgba(0,0,0,.5)" }}
    >
      <div className="panel-h">
        <span className="row xs t3 gap-1.5" aria-hidden="true">
          {[0, 1, 2].map((i) => (
            <i
              key={i}
              className="w-[9px] h-[9px] rounded-full"
              style={{ background: "var(--line-3)" }}
            />
          ))}
        </span>
        <span className="mono xs t3" aria-hidden="true">
          payment-svc · prod
        </span>
        <span className="row gap-2">
          <span className="badge c-crit mono" aria-hidden="true">
            <span className="d" />
            INCIDENT
          </span>
          {motion && (
            <button
              type="button"
              className="btn btn-ghost btn-sm btn-icon"
              aria-label={paused ? t("play") : t("pause")}
              aria-pressed={paused}
              onClick={() => setPaused((p) => !p)}
            >
              {paused ? (
                <Play size={14} aria-hidden="true" />
              ) : (
                <Pause size={14} aria-hidden="true" />
              )}
            </button>
          )}
        </span>
      </div>

      <div className="pad stack gap-3.5" aria-hidden="true">
        <div className="flex items-end gap-[3px] h-[72px]">
          {BARS.map((v, i) => (
            <i
              key={i}
              className="growY block flex-1 rounded-t-[2px]"
              style={{
                height: `${Math.round((v / PEAK) * 100)}%`,
                background:
                  v > 10 ? "color-mix(in srgb, var(--crit) 80%, transparent)" : "var(--line-3)",
                animationDelay: `${300 + i * 16}ms`,
              }}
            />
          ))}
        </div>

        <div
          className="well mono xs grid gap-1.5 min-h-[108px] px-3 py-2.5"
          onMouseEnter={() => setHeld(true)}
          onMouseLeave={() => setHeld(false)}
        >
          {Array.from({ length: VISIBLE }, (_, j) => {
            const [sev, cls, msg] = LINES[(tick + j) % LINES.length];
            const second = (tick + j) % 10;
            return (
              // Keyed by tick so the newest line re-mounts and plays its entrance.
              <div
                key={j === 0 ? `head-${tick}` : `row-${j}`}
                className={`row gap-2.5 flex-nowrap ${j === 0 && tick > 0 ? "inA" : ""}`}
              >
                <span className="t4">{`19:3${second}:0${j}`}</span>
                <span className={`badge mono ${cls} min-w-[52px] justify-center`}>{sev}</span>
                <span className="ellipsis t2">{msg}</span>
              </div>
            );
          })}
        </div>

        <div
          className="card p-3.5"
          style={{ boxShadow: "inset 0 0 0 1px color-mix(in srgb, var(--ai) 40%, transparent)" }}
        >
          <div className="row between mb-2">
            <span className="row xs gap-1.5" style={{ color: "var(--ai)" }}>
              <Sparkles size={13} />
              {t("mockAi")}
            </span>
            <span className="mono xs t3">0.87</span>
          </div>
          <p className="small m-0">{t("mockCause")}</p>
        </div>
      </div>
    </div>
  );
}
