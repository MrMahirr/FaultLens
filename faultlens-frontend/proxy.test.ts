import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { fakeToken } from "@/test/utils";
import { hasSession, proxy, tokenExpiry } from "./proxy";

const NOW = 1_800_000_000;

function tokenExpiringAt(exp: number) {
  const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${encode({ alg: "none" })}.${encode({ sub: "admin", exp })}.`;
}

function request(path: string, cookies: Record<string, string> = {}) {
  const req = new NextRequest(new URL(path, "http://localhost:3000"));
  for (const [name, value] of Object.entries(cookies)) req.cookies.set(name, value);
  return req;
}

describe("proxy session check", () => {
  it("reads the JWT expiry", () => {
    expect(tokenExpiry(tokenExpiringAt(NOW))).toBe(NOW);
    expect(tokenExpiry("garbage")).toBeNull();
    expect(tokenExpiry(fakeToken("admin", "ADMIN"))).toBe(9_999_999_999);
  });

  it("accepts a live token, or an expired one when a refreshable session exists", () => {
    expect(
      hasSession(request("/dashboard", { faultlens_token: tokenExpiringAt(NOW + 60) }), NOW),
    ).toBe(true);
    expect(
      hasSession(request("/dashboard", { faultlens_token: tokenExpiringAt(NOW - 60) }), NOW),
    ).toBe(false);
    expect(
      hasSession(
        request("/dashboard", {
          faultlens_token: tokenExpiringAt(NOW - 60),
          faultlens_session: "1",
        }),
        NOW,
      ),
    ).toBe(true);
    expect(hasSession(request("/dashboard"), NOW)).toBe(false);
  });

  it("sends visitors without a session to /login", () => {
    const response = proxy(request("/alarms"));
    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe("http://localhost:3000/login");
  });
});
