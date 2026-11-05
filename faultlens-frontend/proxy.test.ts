import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { hasSession, proxy } from "./proxy";

const NOW = 1_800_000_000;

function request(path: string, cookies: Record<string, string> = {}) {
  const req = new NextRequest(new URL(path, "http://localhost:3000"));
  for (const [name, value] of Object.entries(cookies)) req.cookies.set(name, value);
  return req;
}

describe("proxy session check", () => {
  it("checks the session expiry, not just the cookie's presence", () => {
    expect(hasSession(request("/dashboard", { faultlens_session: String(NOW + 60) }), NOW)).toBe(
      true,
    );
    expect(hasSession(request("/dashboard", { faultlens_session: String(NOW - 60) }), NOW)).toBe(
      false,
    );
    expect(hasSession(request("/dashboard", { faultlens_session: "1" }), NOW)).toBe(false);
    expect(hasSession(request("/dashboard", { faultlens_session: "garbage" }), NOW)).toBe(false);
    expect(hasSession(request("/dashboard"), NOW)).toBe(false);
  });

  it("ignores a legacy access-token cookie", () => {
    expect(hasSession(request("/dashboard", { faultlens_token: "a.b.c" }), NOW)).toBe(false);
  });

  it("sends visitors without a session to /login and clears stale cookies", () => {
    const response = proxy(request("/alarms", { faultlens_token: "a.b.c" }));
    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe("http://localhost:3000/login");
    expect(response.headers.get("set-cookie")).toContain("faultlens_token=;");
  });

  it("lets a live session through and keeps it off /login", () => {
    const session = { faultlens_session: String(Math.floor(Date.now() / 1000) + 3600) };
    expect(proxy(request("/alarms", session)).headers.get("location")).toBeNull();
    expect(proxy(request("/login", session)).headers.get("location")).toBe(
      "http://localhost:3000/dashboard",
    );
  });
});
