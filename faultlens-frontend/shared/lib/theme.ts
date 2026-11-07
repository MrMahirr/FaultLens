/**
 * Theme resolution shared by the pre-paint bootstrap script and the React side.
 * Preference "system" follows `prefers-color-scheme`, live (see useTheme).
 */

export type ThemePreference = "dark" | "light" | "system";
export type ResolvedTheme = "dark" | "light";
export type MotionPreference = "on" | "off";

/** localStorage key of the persisted UI store (zustand `persist`). */
export const UI_STORE_KEY = "faultlens-ui";

export function resolveTheme(pref: ThemePreference, systemPrefersLight: boolean): ResolvedTheme {
  if (pref === "system") return systemPrefersLight ? "light" : "dark";
  return pref;
}

/**
 * Runs synchronously in <head> before hydration so the first paint already has the right
 * theme and motion setting (no flash). Mirrors resolveTheme; kept dependency-free on purpose.
 */
export const THEME_BOOTSTRAP_SCRIPT = `(function(){try{var s=JSON.parse(localStorage.getItem(${JSON.stringify(
  UI_STORE_KEY,
)})||"{}").state||{};var p=s.theme||"dark";var t=p==="system"?(matchMedia("(prefers-color-scheme: light)").matches?"light":"dark"):p;var d=document.documentElement;d.setAttribute("data-theme",t);d.setAttribute("data-motion",s.motion==="off"?"off":"on");}catch(e){}})();`;
