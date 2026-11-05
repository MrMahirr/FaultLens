import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const publicPaths = ["/login"];

/** Mirrors the cookie names in features/auth/store/auth.store.ts. */
const TOKEN_COOKIE = "faultlens_token";
const SESSION_COOKIE = "faultlens_session";

/**
 * Seconds-since-epoch expiry of a JWT, or null if it cannot be read. Not a verification (the
 * proxy has no key): it only spares an obviously expired session a round trip to the API.
 */
export function tokenExpiry(token: string): number | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    const exp = JSON.parse(json).exp;
    return typeof exp === "number" ? exp : null;
  } catch {
    return null;
  }
}

/**
 * Whether the browser can probably reach the dashboard: a live access token, or an expired one
 * plus the session marker (the client will renew it with the HttpOnly refresh cookie).
 */
export function hasSession(request: NextRequest, nowSec = Math.floor(Date.now() / 1000)): boolean {
  const token = request.cookies.get(TOKEN_COOKIE)?.value;
  const exp = token ? tokenExpiry(token) : null;
  if (exp !== null && exp > nowSec) return true;
  return request.cookies.has(SESSION_COOKIE);
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Exclude static files and api routes
  if (
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api") ||
    pathname === "/favicon.ico" ||
    pathname.includes(".")
  ) {
    return NextResponse.next();
  }

  const signedIn = hasSession(request);

  // If trying to access public auth paths while logged in, redirect to dashboard
  if (pathname === "/login" || pathname === "/") {
    if (signedIn) {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }
    // If accessing root without a session, redirect to login
    if (pathname === "/") {
      return NextResponse.redirect(new URL("/login", request.url));
    }
    return NextResponse.next();
  }

  // Protected paths need a session; drop a stale access-token cookie on the way to /login.
  if (!signedIn && !publicPaths.includes(pathname)) {
    const response = NextResponse.redirect(new URL("/login", request.url));
    response.cookies.delete(TOKEN_COOKIE);
    return response;
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    "/((?!_next/static|_next/image|favicon.ico).*)",
  ],
};
