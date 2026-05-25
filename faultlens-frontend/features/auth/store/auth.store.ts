import { create } from "zustand";
import { persist } from "zustand/middleware";

/* ── Types ─────────────────────────────────────────────────── */

interface AuthUser {
  username: string;
  role: string;
}

interface AuthStore {
  token: string | null;
  user: AuthUser | null;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
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

      isAuthenticated: () => get().token !== null,
    }),
    {
      name: "loglens-auth",
      partialize: (state) => ({
        token: state.token,
        user: state.user,
      }),
    }
  )
);

export type { AuthUser };
