"use client";

import { useEffect, useState } from "react";
import { useUIStore } from "@/shared/store/ui.store";

/**
 * Theme hook — Zustand'dan tema okur ve DOM'u günceller.
 * data-theme attribute'unu HTML element'ine set eder.
 */
export function useTheme() {
  const theme = useUIStore((state) => state.theme);
  const setTheme = useUIStore((state) => state.setTheme);
  const toggleTheme = useUIStore((state) => state.toggleTheme);

  const [resolvedTheme, setResolvedTheme] = useState<"dark" | "light">("dark");

  useEffect(() => {
    const applyTheme = (currentTheme: string) => {
      if (currentTheme === "system") {
        const isSystemDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        document.documentElement.setAttribute("data-theme", isSystemDark ? "dark" : "light");
        setResolvedTheme(isSystemDark ? "dark" : "light");
      } else {
        document.documentElement.setAttribute("data-theme", currentTheme);
        setResolvedTheme(currentTheme as "dark" | "light");
      }
    };

    applyTheme(theme);

    if (theme === "system") {
      const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
      const handleChange = () => applyTheme("system");
      
      mediaQuery.addEventListener("change", handleChange);
      return () => mediaQuery.removeEventListener("change", handleChange);
    }
  }, [theme]);

  return {
    theme,
    setTheme,
    toggleTheme,
    isDark: resolvedTheme === "dark",
    isLight: resolvedTheme === "light",
  };
}
