import { describe, expect, it } from "vitest";
import { getApiErrorCode, getApiErrorMessage } from "./errors";

const axiosError = (data: unknown) => ({ response: { status: 400, data } });

describe("getApiErrorMessage", () => {
  it("reads the ErrorEnvelope shape", () => {
    const error = axiosError({ success: false, error: { code: "X", message: "Geçersiz istek" } });
    expect(getApiErrorMessage(error, "fallback")).toBe("Geçersiz istek");
  });

  it("reads the ApiResponse.message shape", () => {
    expect(getApiErrorMessage(axiosError({ success: false, message: "Kaynak bulunamadı" }), "fallback")).toBe(
      "Kaynak bulunamadı",
    );
  });

  it("falls back for network errors, blank or non-string messages", () => {
    expect(getApiErrorMessage(new Error("Network Error"), "fallback")).toBe("fallback");
    expect(getApiErrorMessage(axiosError({ error: { message: "   " } }), "fallback")).toBe("fallback");
    expect(getApiErrorMessage(axiosError({ message: 42 }), "fallback")).toBe("fallback");
    expect(getApiErrorMessage(undefined, "fallback")).toBe("fallback");
  });
});

describe("getApiErrorCode", () => {
  it("returns the code when present", () => {
    expect(getApiErrorCode(axiosError({ error: { code: "PASSWORD_CHANGE_REQUIRED" } }))).toBe(
      "PASSWORD_CHANGE_REQUIRED",
    );
    expect(getApiErrorCode(axiosError({ message: "x" }))).toBeUndefined();
    expect(getApiErrorCode(null)).toBeUndefined();
  });
});
