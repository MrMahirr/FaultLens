import { afterEach, describe, expect, it, vi } from "vitest";
import { decodeJwt } from "./jwt";

/** Builds an unsigned JWT with a base64url-encoded payload (signature is not checked client-side). */
function token(payload: object): string {
  const encode = (value: object) =>
    Buffer.from(JSON.stringify(value))
      .toString("base64")
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  return `${encode({ alg: "HS256" })}.${encode(payload)}.signature`;
}

describe("decodeJwt", () => {
  afterEach(() => vi.restoreAllMocks());

  it("returns the payload, including roles and exp", () => {
    const exp = Math.floor(Date.now() / 1000) + 3600;
    expect(decodeJwt(token({ sub: "admin", roles: ["ROLE_ADMIN"], iat: 1, exp }))).toEqual({
      sub: "admin",
      roles: ["ROLE_ADMIN"],
      iat: 1,
      exp,
    });
  });

  it("decodes UTF-8 claims", () => {
    expect(decodeJwt(token({ sub: "çağrı-şükrü" }))?.sub).toBe("çağrı-şükrü");
  });

  it("decodes base64url payloads that need padding and contain - or _", () => {
    // "???" encodes to "Pz8_" in base64url — exercises the '_' → '/' replacement.
    expect(decodeJwt(token({ sub: "???", n: 1 }))?.sub).toBe("???");
  });

  it("returns the password-change claim untouched", () => {
    expect(decodeJwt(token({ sub: "admin", pwd_change: true }))?.pwd_change).toBe(true);
  });

  it("returns null for malformed tokens", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    expect(decodeJwt("")).toBeNull();
    expect(decodeJwt("only-one-part")).toBeNull();
    expect(decodeJwt("a.%%%not-base64%%%.c")).toBeNull();
    expect(decodeJwt(`a.${Buffer.from("not json").toString("base64")}.c`)).toBeNull();
  });
});
