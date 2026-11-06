"use client";

import { useEffect, type ReactNode } from "react";
import { MotionConfig } from "framer-motion";
import { useTheme } from "@/shared/hooks/useTheme";
import { useUIStore } from "@/shared/store/ui.store";

/**
 * Writes data-theme / data-motion on <html> (the bootstrap script in the root layout does
 * the same before first paint) and turns framer-motion animations off when the user chose
 * "reduce motion" in Settings, or the OS asks for reduced motion.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const { resolvedTheme } = useTheme();
  const motion = useUIStore((state) => state.motion);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", resolvedTheme);
  }, [resolvedTheme]);

  useEffect(() => {
    document.documentElement.setAttribute("data-motion", motion);
  }, [motion]);

  return (
    <MotionConfig reducedMotion={motion === "off" ? "always" : "user"}>{children}</MotionConfig>
  );
}
