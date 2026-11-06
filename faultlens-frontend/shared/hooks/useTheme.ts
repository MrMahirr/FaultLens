"use client";

import { useSyncExternalStore } from "react";
import { useUIStore } from "@/shared/store/ui.store";
import { resolveTheme } from "@/shared/lib/theme";

const LIGHT_QUERY = "(prefers-color-scheme: light)";

function subscribe(onChange: () => void) {
  const mq = window.matchMedia(LIGHT_QUERY);
  mq.addEventListener("change", onChange);
  return () => mq.removeEventListener("change", onChange);
}

const getSnapshot = () => window.matchMedia(LIGHT_QUERY).matches;
const getServerSnapshot = () => false;

/**
 * Theme preference (dark / light / system) and the theme actually in effect. "system"
 * follows the OS setting live. The DOM attribute itself is written by ThemeProvider.
 */
export function useTheme() {
  const theme = useUIStore((state) => state.theme);
  const setTheme = useUIStore((state) => state.setTheme);
  const systemPrefersLight = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  const resolvedTheme = resolveTheme(theme, systemPrefersLight);

  return {
    theme,
    resolvedTheme,
    setTheme,
    toggleTheme: () => setTheme(resolvedTheme === "dark" ? "light" : "dark"),
    isDark: resolvedTheme === "dark",
    isLight: resolvedTheme === "light",
  };
}
