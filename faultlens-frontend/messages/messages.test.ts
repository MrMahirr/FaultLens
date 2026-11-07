import { describe, expect, it } from "vitest";
import { MESSAGES } from "./index";

/** Dotted paths of every leaf string. */
function keys(tree: object, prefix = ""): string[] {
  return Object.entries(tree).flatMap(([key, value]) =>
    typeof value === "object" && value !== null
      ? keys(value, `${prefix}${key}.`)
      : [`${prefix}${key}`],
  );
}

/** ICU arguments ({name}, {count, plural, …}) used by a message; plural branches are not arguments. */
function placeholders(message: string): string[] {
  const names = [...message.matchAll(/\{\s*(\w+)\s*[},]/g)].map((m) => m[1]);
  return [...new Set(names)].sort();
}

function leaf(tree: object, path: string): string {
  return path
    .split(".")
    .reduce<unknown>((node, k) => (node as Record<string, unknown>)[k], tree) as string;
}

describe("messages", () => {
  it("has the same keys in every language", () => {
    expect(keys(MESSAGES.en).sort()).toEqual(keys(MESSAGES.tr).sort());
  });

  it("uses the same placeholders in every language", () => {
    for (const path of keys(MESSAGES.tr)) {
      expect(placeholders(leaf(MESSAGES.en, path)), path).toEqual(
        placeholders(leaf(MESSAGES.tr, path)),
      );
    }
  });

  it("has no empty messages", () => {
    for (const locale of ["tr", "en"] as const) {
      for (const path of keys(MESSAGES[locale])) {
        expect(leaf(MESSAGES[locale], path).trim(), `${locale}:${path}`).not.toBe("");
      }
    }
  });
});
