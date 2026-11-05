import { describe, expect, it } from "vitest";
import { camelToSnake, snakeToCamel } from "./caseTransform";

describe("snakeToCamel", () => {
  it("converts nested objects and arrays", () => {
    expect(
      snakeToCamel({
        total_elements: 2,
        content: [{ source_id: 1, first_seen_at: "x", stack_trace: null }],
        page_info: { total_pages: 1 },
      }),
    ).toEqual({
      totalElements: 2,
      content: [{ sourceId: 1, firstSeenAt: "x", stackTrace: null }],
      pageInfo: { totalPages: 1 },
    });
  });

  it("returns primitives, null and undefined unchanged", () => {
    expect(snakeToCamel("a_b")).toBe("a_b");
    expect(snakeToCamel(5)).toBe(5);
    expect(snakeToCamel(null)).toBeNull();
    expect(snakeToCamel(undefined)).toBeUndefined();
  });

  it("leaves keys that are already camelCase or UPPER_CASE alone", () => {
    // /logs/stats returns severity names as keys; only `_<lowercase>` is rewritten.
    expect(snakeToCamel({ ERROR: 1, TRACE: 0, total: 1, alreadyCamel: true })).toEqual({
      ERROR: 1,
      TRACE: 0,
      total: 1,
      alreadyCamel: true,
    });
  });

  it("keeps Date instances intact", () => {
    const date = new Date("2026-11-04T00:00:00Z");
    expect(snakeToCamel<{ createdAt: Date }>({ created_at: date }).createdAt).toBe(date);
  });
});

describe("camelToSnake", () => {
  it("converts request bodies deeply", () => {
    expect(
      camelToSnake({
        currentPassword: "a",
        newPassword: "b",
        nested: [{ logFilePath: "/var/log" }],
      }),
    ).toEqual({
      current_password: "a",
      new_password: "b",
      nested: [{ log_file_path: "/var/log" }],
    });
  });

  it("round-trips with snakeToCamel", () => {
    const body = { serviceName: "orders", deployedAt: "2026-11-04", deployedBy: "ci" };
    expect(snakeToCamel(camelToSnake(body))).toEqual(body);
  });
});
