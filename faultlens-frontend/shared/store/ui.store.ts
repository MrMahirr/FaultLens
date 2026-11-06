import { create } from "zustand";
import { persist } from "zustand/middleware";
import { UI_STORE_KEY, type MotionPreference, type ThemePreference } from "@/shared/lib/theme";

/* ── Types ─────────────────────────────────────────────────── */

interface UIStore {
  sidebarOpen: boolean;
  theme: ThemePreference;
  motion: MotionPreference;
  compactMode: boolean;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  setTheme: (theme: ThemePreference) => void;
  setMotion: (motion: MotionPreference) => void;
  toggleCompactMode: () => void;
  setCompactMode: (compact: boolean) => void;
}

/* ── Store ──────────────────────────────────────────────────── */

export const useUIStore = create<UIStore>()(
  persist(
    (set) => ({
      sidebarOpen: true,
      theme: "dark",
      motion: "on",
      compactMode: false,

      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setSidebarOpen: (open) => set({ sidebarOpen: open }),
      setTheme: (theme) => set({ theme }),
      setMotion: (motion) => set({ motion }),
      toggleCompactMode: () => set((state) => ({ compactMode: !state.compactMode })),
      setCompactMode: (compact) => set({ compactMode: compact }),
    }),
    {
      name: UI_STORE_KEY,
      partialize: (state) => ({
        sidebarOpen: state.sidebarOpen,
        theme: state.theme,
        motion: state.motion,
        compactMode: state.compactMode,
      }),
    },
  ),
);
