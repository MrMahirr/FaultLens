"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { AlertTriangle, X } from "lucide-react";
import { useIsClient } from "@/shared/hooks/useIsClient";
import { useWebSocket } from "@/shared/hooks/useWebSocket";
import { Endpoints } from "@/shared/api/endpoints";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useUIStore } from "@/shared/store/ui.store";

/** In-app alarm broadcast sent over /ws/logs (InAppNotificationService). */
interface AlarmSocketMessage {
  type?: string;
  title?: string;
  message?: string;
}

/** Short two-tone chime (Web Audio); silent where audio is unavailable. */
function playNotificationSound() {
  try {
    const Ctx =
      window.AudioContext ||
      (window as Window & { webkitAudioContext?: typeof window.AudioContext }).webkitAudioContext;
    if (!Ctx) return;
    const audioCtx = new Ctx();
    const oscillator = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    oscillator.type = "sine";
    oscillator.frequency.setValueAtTime(880, audioCtx.currentTime);
    oscillator.frequency.exponentialRampToValueAtTime(440, audioCtx.currentTime + 0.1);
    gain.gain.setValueAtTime(0, audioCtx.currentTime);
    gain.gain.linearRampToValueAtTime(0.5, audioCtx.currentTime + 0.05);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.5);
    oscillator.connect(gain);
    gain.connect(audioCtx.destination);
    oscillator.start(audioCtx.currentTime);
    oscillator.stop(audioCtx.currentTime + 0.5);
  } catch (e) {
    console.error("Audio playback failed", e);
  }
}

export function GlobalNotificationListener() {
  const t = useTranslations("shell");
  const mounted = useIsClient();
  const token = useAuthStore((state) => state.token);

  const handleMessage = (raw: unknown) => {
    const data = raw as AlarmSocketMessage | null;
    if (data?.type !== "ALARM") return;
    playNotificationSound();
    const reduced = useUIStore.getState().motion === "off";
    toast.custom(
      (item) => (
        <div
          role="alert"
          className="card flex items-start gap-3 w-[min(420px,calc(100vw-32px))] p-3 pl-3.5"
          style={{
            boxShadow: "var(--pop), inset 3px 0 0 var(--crit)",
            opacity: item.visible || reduced ? 1 : 0,
            transition: "opacity var(--t-2) var(--ease)",
          }}
        >
          <AlertTriangle size={16} className="text-critical mt-0.5 shrink-0" aria-hidden="true" />
          <div className="flex-1 min-w-0">
            <p className="small font-medium">{data.title || t("alarmToastTitle")}</p>
            <p className="xs t2 mt-0.5 break-words">{data.message || t("alarmToastFallback")}</p>
          </div>
          <button
            type="button"
            onClick={() => toast.dismiss(item.id)}
            className="btn btn-ghost btn-sm btn-icon -mr-1 -mt-1"
            aria-label={t("dismiss")}
          >
            <X size={14} aria-hidden="true" />
          </button>
        </div>
      ),
      { duration: 8000, position: "top-right" },
    );
  };

  useWebSocket({
    url: Endpoints.WS.LOGS,
    token,
    onMessage: handleMessage,
    enabled: mounted && !!token,
  });

  return null;
}
