"use client";

import { useCallback, useSyncExternalStore } from "react";

/**
 * Live result of a CSS media query. `serverValue` is used before hydration (and on the
 * server), so pick the value that matches the server-rendered markup.
 */
export function useMediaQuery(query: string, serverValue = false): boolean {
  const subscribe = useCallback(
    (onChange: () => void) => {
      const mq = window.matchMedia(query);
      mq.addEventListener("change", onChange);
      return () => mq.removeEventListener("change", onChange);
    },
    [query],
  );
  return useSyncExternalStore(
    subscribe,
    () => window.matchMedia(query).matches,
    () => serverValue,
  );
}

/** Tailwind `lg` (1024px): the sidebar is a rail from here up, a drawer below. */
export const DESKTOP_QUERY = "(min-width: 1024px)";
