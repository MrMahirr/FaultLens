import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { rangeStart } from "../types/log.types";
import { LogApi } from "./logApi";

const page = { content: [], page: 0, size: 20, total_elements: 0, total_pages: 0 };

function captureFrom() {
  const seen: URLSearchParams[] = [];
  server.use(
    http.get(`${API}/logs`, ({ request }) => {
      seen.push(new URL(request.url).searchParams);
      return HttpResponse.json(ok(page));
    }),
  );
  return seen;
}

describe("log list time range (P08)", () => {
  it("bounds every query to the last 24 hours by default", async () => {
    const seen = captureFrom();
    const before = Date.now();

    await LogApi.getLogs({ search: "timeout" });

    const from = Date.parse(seen[0].get("from")!);
    expect(before - from).toBeGreaterThanOrEqual(86_400_000 - 1000);
    expect(before - from).toBeLessThan(86_400_000 + 5000);
    expect(seen[0].get("search")).toBe("timeout");
  });

  it("uses the chosen range, and 'all' only as an explicit choice", async () => {
    const seen = captureFrom();

    await LogApi.getLogs({ range: "7d" });
    await LogApi.getLogs({ range: "all" });
    await LogApi.getLogs({ range: "1h", startDate: "2026-11-01T00:00:00.000Z" });

    expect(Date.now() - Date.parse(seen[0].get("from")!)).toBeGreaterThan(6.9 * 86_400_000);
    expect(seen[1].get("from")).toBe(new Date(0).toISOString());
    expect(seen[2].get("from")).toBe("2026-11-01T00:00:00.000Z"); // explicit start wins
  });

  it("computes window starts", () => {
    const now = Date.parse("2026-11-06T12:00:00.000Z");
    expect(rangeStart("1h", now)).toBe("2026-11-06T11:00:00.000Z");
    expect(rangeStart("30d", now)).toBe("2026-10-07T12:00:00.000Z");
  });
});
