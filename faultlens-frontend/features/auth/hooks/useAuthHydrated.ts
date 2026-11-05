"use client";

import { useSyncExternalStore } from "react";
import { useAuthStore } from "@/features/auth/store/auth.store";

/**
 * True once the persisted auth state has been read from localStorage.
 *
 * During React hydration zustand returns the store's *initial* state (`token: null`), so a
 * component that redirects on "no token" must wait for this flag; otherwise every hard load
 * or deep link to a protected page is sent to /login (and from there to /dashboard).
 */
export function useAuthHydrated(): boolean {
  return useSyncExternalStore(
    (onChange) => useAuthStore.persist.onFinishHydration(onChange),
    () => useAuthStore.persist.hasHydrated(),
    () => false,
  );
}
