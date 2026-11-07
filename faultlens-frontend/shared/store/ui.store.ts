import { create } from "zustand";
import { persist } from "zustand/middleware";
import { UI_STORE_KEY, type MotionPreference, type ThemePreference } from "@/shared/lib/theme";

/* ── Types ─────────────────────────────────────────────────── */

interface UIStore {
  /** Desktop (lg+): expanded or icon-only rail. Persisted. */
  sidebarOpen: boolean;
  /** Phones/tablets (< lg): the navigation drawer is open. Never persisted. */
  mobileNavOpen: boolean;
  theme: ThemePreference;
  motion: MotionPreference;
  compactMode: boolean;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  setMobileNavOpen: (open: boolean) => void;
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
      mobileNavOpen: false,
      theme: "dark",
      motion: "on",
      compactMode: false,

      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setSidebarOpen: (open) => set({ sidebarOpen: open }),
      setMobileNavOpen: (open) => set({ mobileNavOpen: open }),
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
