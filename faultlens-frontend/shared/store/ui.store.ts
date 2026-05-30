import { create } from "zustand";
import { persist } from "zustand/middleware";

/* ── Types ─────────────────────────────────────────────────── */

type Theme = "dark" | "light" | "system";

interface UIStore {
  sidebarOpen: boolean;
  theme: Theme;
  compactMode: boolean;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
  toggleCompactMode: () => void;
  setCompactMode: (compact: boolean) => void;
}

/* ── Store ──────────────────────────────────────────────────── */

export const useUIStore = create<UIStore>()(
  persist(
    (set) => ({
      sidebarOpen: true,
      theme: "dark",
      compactMode: false,

      toggleSidebar: () =>
        set((state) => ({ sidebarOpen: !state.sidebarOpen })),

      setSidebarOpen: (open) => set({ sidebarOpen: open }),

      setTheme: (theme) => set({ theme }),

      toggleTheme: () =>
        set((state) => ({
          theme: state.theme === "dark" ? "light" : "dark",
        })),

      toggleCompactMode: () => 
        set((state) => ({ compactMode: !state.compactMode })),
        
      setCompactMode: (compact) => set({ compactMode: compact }),
    }),
    {
      name: "faultlens-ui",
      partialize: (state) => ({
        sidebarOpen: state.sidebarOpen,
        theme: state.theme,
        compactMode: state.compactMode,
      }),
    },
  ),
);
