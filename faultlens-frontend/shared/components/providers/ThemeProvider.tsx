"use client";

import { useEffect, type ReactNode } from "react";
import { useTheme } from "@/shared/hooks/useTheme";

interface ThemeProviderProps {
  children: ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const { theme } = useTheme();

  // Sync theme on mount (handles SSR → client hydration)
  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  return <>{children}</>;
}
