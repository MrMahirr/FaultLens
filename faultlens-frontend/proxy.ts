import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const publicPaths = ["/login"];

/** Mirrors features/auth/store/auth.store.ts. */
const SESSION_COOKIE = "faultlens_session";
const LEGACY_TOKEN_COOKIE = "faultlens_token";

/**
 * Whether the browser has an unexpired session: the marker cookie holds the epoch second its
 * refresh cookie expires. Not a verification (the refresh cookie is HttpOnly and scoped to the
 * API); it only spares an expired session a round trip, the API stays the authority.
 */
export function hasSession(request: NextRequest, nowSec = Math.floor(Date.now() / 1000)): boolean {
  const expiresAt = Number(request.cookies.get(SESSION_COOKIE)?.value);
  return Number.isFinite(expiresAt) && expiresAt > nowSec;
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

  // Protected paths need a session; drop stale cookies on the way to /login.
  if (!signedIn && !publicPaths.includes(pathname)) {
    const response = NextResponse.redirect(new URL("/login", request.url));
    response.cookies.delete(SESSION_COOKIE);
    response.cookies.delete(LEGACY_TOKEN_COOKIE);
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
