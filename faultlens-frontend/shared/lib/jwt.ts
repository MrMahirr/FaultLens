/**
 * Simple JWT decoder for frontend.
 * Extracs payload without verifying signature (since verification happens on backend).
 */

export interface JwtPayload {
  sub: string;
  roles: string[];
  iat: number;
  exp: number;
  [key: string]: any;
}

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    
    return JSON.parse(jsonPayload) as JwtPayload;
  } catch (error) {
    console.error("JWT decoding failed:", error);
    return null;
  }
}
