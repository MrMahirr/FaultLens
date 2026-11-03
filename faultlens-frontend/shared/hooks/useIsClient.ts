"use client";

import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * False during server rendering and hydration, true afterwards.
 *
 * Replaces the `const [mounted, setMounted] = useState(false); useEffect(() =>
 * setMounted(true), [])` pattern, which renders twice and is flagged by
 * react-hooks/set-state-in-effect.
 */
export function useIsClient(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
}
