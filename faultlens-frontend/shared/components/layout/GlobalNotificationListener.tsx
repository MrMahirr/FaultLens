"use client";

import { useEffect, useState } from "react";
import { useWebSocket } from "@/shared/hooks/useWebSocket";
import { Endpoints } from "@/shared/api/endpoints";
import toast from "react-hot-toast";
import { AlertTriangle } from "lucide-react";

export function GlobalNotificationListener() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Play a soft chime sound using Web Audio API
  const playNotificationSound = () => {
    try {
      const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContext) return;
      
      const audioCtx = new AudioContext();
      const oscillator = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();

      oscillator.type = "sine";
      oscillator.frequency.setValueAtTime(880, audioCtx.currentTime); // A5 note
      oscillator.frequency.exponentialRampToValueAtTime(440, audioCtx.currentTime + 0.1); // Drop to A4
      
      gainNode.gain.setValueAtTime(0, audioCtx.currentTime);
      gainNode.gain.linearRampToValueAtTime(0.5, audioCtx.currentTime + 0.05); // Fade in quickly
      gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.5); // Fade out smoothly

      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);

      oscillator.start(audioCtx.currentTime);
      oscillator.stop(audioCtx.currentTime + 0.5);
    } catch (e) {
      console.error("Audio playback failed", e);
    }
  };

  const handleMessage = (data: any) => {
    if (data?.type === "ALARM") {
      playNotificationSound();
      toast.custom(
        (t) => (
          <div
            className={`${
              t.visible ? "animate-enter" : "animate-leave"
            } max-w-md w-full bg-error text-white shadow-2xl rounded-2xl pointer-events-auto flex ring-1 ring-black ring-opacity-5`}
          >
            <div className="flex-1 w-0 p-4">
              <div className="flex items-start">
                <div className="flex-shrink-0 pt-0.5">
                  <AlertTriangle className="h-10 w-10 text-white/90" />
                </div>
                <div className="ml-3 flex-1">
                  <p className="text-sm font-bold text-white uppercase tracking-wider">
                    {data.title || "Sistem Uyarısı"}
                  </p>
                  <p className="mt-1 text-sm text-white/90">
                    {data.message || "Bilinmeyen bir hata oluştu."}
                  </p>
                </div>
              </div>
            </div>
            <div className="flex border-l border-white/20">
              <button
                onClick={() => toast.dismiss(t.id)}
                className="w-full border border-transparent rounded-none rounded-r-2xl p-4 flex items-center justify-center text-sm font-medium hover:bg-white/10 focus:outline-none transition-colors"
              >
                Kapat
              </button>
            </div>
          </div>
        ),
        { duration: 8000, position: "top-right" }
      );
    }
  };

  useWebSocket({
    url: Endpoints.WS.LOGS,
    onMessage: handleMessage,
    enabled: mounted,
  });

  return null; // This component doesn't render anything
}
