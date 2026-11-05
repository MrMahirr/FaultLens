"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useAuthHydrated } from "@/features/auth/hooks/useAuthHydrated";
import { refreshSession } from "@/shared/api/session";

export type SessionStatus = "loading" | "authenticated" | "anonymous";

/**
 * Session state for route guards.
 *
 * The access token lives only in memory (P07), so after a page load the store knows the user
 * (persisted) but has no token yet: the session is restored with the HttpOnly refresh cookie
 * before the page renders. "anonymous" means there is nothing to restore (or it failed).
 */
export function useSession(): SessionStatus {
  const hydrated = useAuthHydrated();
  const token = useAuthStore((state) => state.token);
  const hasUser = useAuthStore((state) => state.user !== null);
  const [restoreFailed, setRestoreFailed] = useState(false);

  useEffect(() => {
    if (!hydrated || token || !hasUser) return;
    let active = true;
    void refreshSession().then((fresh) => {
      if (active && !fresh) setRestoreFailed(true);
    });
    return () => {
      active = false;
    };
  }, [hydrated, token, hasUser]);

  if (!hydrated) return "loading";
  if (token) return "authenticated";
  if (hasUser && !restoreFailed) return "loading";
  return "anonymous";
}
