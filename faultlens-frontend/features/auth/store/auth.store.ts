import { create } from "zustand";
import { persist } from "zustand/middleware";
/**
 * Session marker read by proxy.ts. Its value is the epoch second the refresh cookie expires; it
 * is not a credential. The access token itself is never written to a cookie or localStorage
 * (P07): it lives in memory and is restored from the HttpOnly refresh cookie after a reload.
 */
export const SESSION_COOKIE = "faultlens_session";
/** Matches the backend refresh-token lifetime (faultlens.auth.refresh-ttl, 7 days). */
const SESSION_MAX_AGE_SEC = 7 * 24 * 60 * 60;
/** Written by earlier versions (the access token itself); removed on sight. */
const LEGACY_TOKEN_COOKIE = "faultlens_token";

function writeSessionCookie() {
  const expiresAt = Math.floor(Date.now() / 1000) + SESSION_MAX_AGE_SEC;
  document.cookie = `${SESSION_COOKIE}=${expiresAt}; path=/; max-age=${SESSION_MAX_AGE_SEC}; SameSite=Lax`;
  document.cookie = `${LEGACY_TOKEN_COOKIE}=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;`;
}

function clearSessionCookies() {
  for (const name of [SESSION_COOKIE, LEGACY_TOKEN_COOKIE]) {
    document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;`;
  }
}

/* ── Types ─────────────────────────────────────────────────── */

interface AuthUser {
  username: string;
  email?: string;
  role: string;
  /** Mirrors the backend flag; the app only allows /change-password while true. */
  mustChangePassword?: boolean;
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

      login: (token, user) => {
        writeSessionCookie();
        set({ token, user });
      },

      logout: () => {
        clearSessionCookies();
        set({ token: null, user: null });
      },

      updateUser: (updates) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...updates } : null,
        })),

      isAuthenticated: () => get().token !== null,
    }),
    {
      name: "faultlens-auth",
      // Only who is signed in survives a reload; the token is renewed via the refresh cookie.
      version: 1,
      partialize: (state) => ({ user: state.user }),
      // v0 stored the access token too: drop it.
      migrate: (persisted) => ({ user: (persisted as { user?: AuthUser | null })?.user ?? null }),
    },
  ),
);

export type { AuthUser };
