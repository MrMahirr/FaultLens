"use client";

import { useSyncExternalStore } from "react";

const REDUCED = "(prefers-reduced-motion: reduce)";

function subscribe(onChange: () => void) {
  const mq = window.matchMedia(REDUCED);
  mq.addEventListener("change", onChange);
  // The motion setting lives on <html data-motion> (THEME_BOOTSTRAP_SCRIPT, ThemeProvider).
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["data-motion"],
  });
  return () => {
    mq.removeEventListener("change", onChange);
    observer.disconnect();
  };
}

function getSnapshot() {
  return document.documentElement.dataset.motion !== "off" && !window.matchMedia(REDUCED).matches;
}

/**
 * Whether decorative motion may run: the in-app motion setting is on and the OS does not ask
 * for reduced motion. False on the server, so nothing animates before hydration decides.
 */
export function useMotionAllowed(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot, () => false);
}
