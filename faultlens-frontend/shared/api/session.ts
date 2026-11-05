import axios from "axios";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { Endpoints } from "@/shared/api/endpoints";

/**
 * Session renewal (P07).
 *
 * The access token lives 15 minutes. The refresh token is an HttpOnly cookie scoped to
 * /api/v1/auth that this code never sees: the browser sends it with `withCredentials`, the
 * backend rotates it and returns a new access token.
 *
 * Single flight: many requests can fail with 401 at once (dashboard queries); they all wait for
 * the same refresh. Refresh tokens are single-use, so parallel refreshes would log the user out.
 */
let inFlight: Promise<string | null> | null = null;

/** Endpoints whose 401 means "bad credentials", never "access token expired". */
export function isAuthEndpoint(url: string | undefined): boolean {
  return (
    !!url &&
    [Endpoints.AUTH.LOGIN, Endpoints.AUTH.REFRESH, Endpoints.AUTH.LOGOUT].some((endpoint) =>
      url.startsWith(endpoint),
    )
  );
}

/** A new access token, or null when the session is over (the store is then logged out). */
export function refreshSession(): Promise<string | null> {
  inFlight ??= axios
    .post(Endpoints.AUTH.REFRESH, null, { withCredentials: true, timeout: 10_000 })
    .then((response) => {
      const token: string | undefined = response.data?.data?.token;
      if (!token) return null;
      const { user, login } = useAuthStore.getState();
      if (user) login(token, user);
      return token;
    })
    .catch(() => null)
    .finally(() => {
      inFlight = null;
    });
  return inFlight;
}

/** Revokes the refresh cookie server-side; best effort (the local session ends regardless). */
export function revokeSession(): void {
  void axios
    .post(Endpoints.AUTH.LOGOUT, null, { withCredentials: true, timeout: 5_000 })
    .catch(() => undefined);
}
