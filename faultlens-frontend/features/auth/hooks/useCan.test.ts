import { describe, expect, it } from "vitest";
import { fakeToken } from "@/test/utils";
import { can, roleOf } from "./useCan";

describe("roleOf", () => {
  it("trusts the token claim over the stored user", () => {
    expect(roleOf(fakeToken("ali", "EDITOR"), "ADMIN")).toBe("EDITOR");
    expect(roleOf(null, "ADMIN")).toBe("ADMIN");
  });

  it("treats unknown or missing roles as VIEWER", () => {
    expect(roleOf(null, undefined)).toBe("VIEWER");
    expect(roleOf(null, "OWNER")).toBe("VIEWER");
  });
});

describe("can", () => {
  it("mirrors the backend role matrix", () => {
    expect(can("ADMIN", "delete")).toBe(true);
    expect(can("ADMIN", "manageUsers")).toBe(true);
    expect(can("EDITOR", "edit")).toBe(true);
    expect(can("EDITOR", "delete")).toBe(false);
    expect(can("EDITOR", "manageUsers")).toBe(false);
    expect(can("VIEWER", "edit")).toBe(false);
  });
});
