import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merge Tailwind CSS classes with conflict resolution.
 * clsx handles conditional classes, twMerge resolves Tailwind conflicts.
 *
 * @example
 * cn("px-4 py-2", isActive && "bg-accent", "px-6") → "py-2 px-6 bg-accent"
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

// Dates and numbers: use useDateFormat() (locale-aware, next-intl) instead of ad-hoc helpers.
