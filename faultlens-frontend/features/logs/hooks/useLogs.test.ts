import { act, renderHook } from "@/test/rtl";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Severity } from "@/shared/types/common.types";
import { LOG_PAGE_SIZE, useLogs } from "./useLogs";

const replace = vi.fn();
let query = "";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/logs",
  useSearchParams: () => new URLSearchParams(query),
}));

describe("useLogs (filters in the URL)", () => {
  beforeEach(() => {
    replace.mockReset();
    query = "";
  });

  it("reads filters from the query string", () => {
    query = "q=timeout&sev=ERROR&src=3&range=7d&page=2&group=12&from=2026-11-06T10:00:00Z";
    const { result } = renderHook(() => useLogs());

    expect(result.current.filters).toEqual({
      search: "timeout",
      severity: Severity.ERROR,
      source: "3",
      range: "7d",
      page: 1, // the URL is 1-based, the API 0-based
      size: LOG_PAGE_SIZE,
      groupId: 12,
      startDate: "2026-11-06T10:00:00.000Z",
    });
    expect(result.current.searchInput).toBe("timeout");
  });

  it("ignores invalid values instead of sending them to the API", () => {
    query = "sev=LOUD&range=forever&page=-4&group=abc&from=yesterday";
    const { result } = renderHook(() => useLogs());

    expect(result.current.filters).toMatchObject({
      severity: undefined,
      range: "24h",
      page: 0,
      groupId: undefined,
      startDate: undefined,
    });
  });

  it("writes a filter change to the URL and goes back to page 1", () => {
    query = "page=3&src=2";
    const { result } = renderHook(() => useLogs());

    act(() => result.current.updateFilter("severity", Severity.CRITICAL));

    expect(replace).toHaveBeenCalledWith("/logs?src=2&sev=CRITICAL", { scroll: false });
  });

  it("drops default values from the URL", () => {
    query = "range=7d";
    const { result } = renderHook(() => useLogs());

    act(() => result.current.updateFilter("range", "24h"));

    expect(replace).toHaveBeenCalledWith("/logs", { scroll: false });
  });
});
