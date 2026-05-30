import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const publicPaths = ['/login'];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Exclude static files and api routes
  if (
    pathname.startsWith('/_next') || 
    pathname.startsWith('/api') || 
    pathname === '/favicon.ico' || 
    pathname.includes('.')
  ) {
    return NextResponse.next();
  }

  // Check auth token cookie
  const token = request.cookies.get('faultlens_token')?.value;

  // If trying to access public auth paths while logged in, redirect to dashboard
  if (pathname === '/login' || pathname === '/') {
    if (token) {
      return NextResponse.redirect(new URL('/dashboard', request.url));
    }
    // If accessing root without token, redirect to login
    if (pathname === '/' && !token) {
      return NextResponse.redirect(new URL('/login', request.url));
    }
    return NextResponse.next();
  }

  // If trying to access protected paths without token, redirect to login
  if (!token && !publicPaths.includes(pathname)) {
    return NextResponse.redirect(new URL('/login', request.url));
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
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
};
