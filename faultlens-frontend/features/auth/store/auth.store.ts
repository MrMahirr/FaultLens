import { create } from "zustand";
import { persist } from "zustand/middleware";

/* ── Types ─────────────────────────────────────────────────── */

interface AuthUser {
  username: string;
  email?: string;
  role: string;
}

interface AuthStore {
  token: string | null;
  user: AuthUser | null;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
  updateUser: (updates: Partial<AuthUser>) => void;
  isAuthenticated: () => boolean;
}

/* ── Store ──────────────────────────────────────────────────── */

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,

      login: (token, user) => set({ token, user }),

      logout: () => set({ token: null, user: null }),

      updateUser: (updates) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...updates } : null,
        })),

      isAuthenticated: () => get().token !== null,
    }),
    {
      name: "faultlens-auth",
      partialize: (state) => ({
        token: state.token,
        user: state.user,
      }),
    },
  ),
);

export type { AuthUser };
