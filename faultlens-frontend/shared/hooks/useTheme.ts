"use client";

import { useEffect } from "react";
import { useUIStore } from "@/shared/store/ui.store";

/**
 * Theme hook — Zustand'dan tema okur ve DOM'u günceller.
 * data-theme attribute'unu HTML element'ine set eder.
 */
export function useTheme() {
  const theme = useUIStore((state) => state.theme);
  const setTheme = useUIStore((state) => state.setTheme);
  const toggleTheme = useUIStore((state) => state.toggleTheme);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  return {
    theme,
    setTheme,
    toggleTheme,
    isDark: theme === "dark",
    isLight: theme === "light",
  };
}
