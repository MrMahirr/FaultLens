"use client";

import { useCallback, useMemo } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

type Updates = Record<string, string | number | null | undefined>;

/**
 * Filter state kept in the URL query string, so a filtered view can be shared as a link and
 * survives reload / back-forward. Empty values are removed; `replace` (not push) keeps the
 * history clean while typing.
 */
export function useUrlState() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const query = searchParams?.toString() ?? "";

  const params = useMemo(() => new URLSearchParams(query), [query]);

  const update = useCallback(
    (updates: Updates) => {
      const next = new URLSearchParams(query);
      for (const [key, value] of Object.entries(updates)) {
        if (value === undefined || value === null || value === "") next.delete(key);
        else next.set(key, String(value));
      }
      const qs = next.toString();
      if (qs === query) return;
      router.replace(qs ? `${pathname}?${qs}` : (pathname ?? "/"), { scroll: false });
    },
    [query, pathname, router],
  );

  const clear = useCallback(
    (keys: string[]) => update(Object.fromEntries(keys.map((k) => [k, undefined]))),
    [update],
  );

  return { params, update, clear };
}
