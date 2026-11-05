import { create } from "zustand";
import { persist } from "zustand/middleware";
import { decodeJwt } from "@/shared/lib/jwt";

/** Read by proxy.ts: the access token (checked for expiry) and the long-lived session marker. */
export const TOKEN_COOKIE = "faultlens_token";
export const SESSION_COOKIE = "faultlens_session";
/** Matches the backend refresh-token lifetime (faultlens.auth.refresh-ttl, 7 days). */
const SESSION_MAX_AGE_SEC = 7 * 24 * 60 * 60;

function writeSessionCookies(token: string) {
  const exp = decodeJwt(token)?.exp;
  const tokenMaxAge = exp ? Math.max(0, exp - Math.floor(Date.now() / 1000)) : 900;
  document.cookie = `${TOKEN_COOKIE}=${token}; path=/; max-age=${tokenMaxAge}; SameSite=Lax`;
  // Not a credential: tells the proxy a refresh cookie probably exists, so an expired access
  // token means "renew" rather than "log in again".
  document.cookie = `${SESSION_COOKIE}=1; path=/; max-age=${SESSION_MAX_AGE_SEC}; SameSite=Lax`;
}

function clearSessionCookies() {
  for (const name of [TOKEN_COOKIE, SESSION_COOKIE]) {
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
        writeSessionCookies(token);
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
      partialize: (state) => ({
        token: state.token,
        user: state.user,
      }),
    },
  ),
);

export type { AuthUser };
