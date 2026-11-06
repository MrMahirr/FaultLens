"use client";

import { useMemo } from "react";
import { useFormatter, useNow } from "next-intl";

type DateInput = string | number | Date | null | undefined;

function toDate(value: DateInput): Date | null {
  if (value === null || value === undefined || value === "") return null;
  const d = value instanceof Date ? value : new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

/**
 * Locale-aware date helpers (Intl via next-intl, so tr/en follow the UI language and the
 * configured time zone). Missing or invalid dates render as "—".
 */
export function useDateFormat() {
  const format = useFormatter();
  // Re-renders relative times once a minute.
  const now = useNow({ updateInterval: 60_000 });

  return useMemo(() => {
    const guard = (value: DateInput, fn: (d: Date) => string) => {
      const d = toDate(value);
      return d ? fn(d) : "—";
    };
    return {
      /** 6 Kas 2026 14:03 / 6 Nov 2026, 14:03 */
      dateTime: (value: DateInput) =>
        guard(value, (d) => format.dateTime(d, { dateStyle: "medium", timeStyle: "short" })),
      /** With seconds, for log timestamps. */
      dateTimeLong: (value: DateInput) =>
        guard(value, (d) => format.dateTime(d, { dateStyle: "medium", timeStyle: "medium" })),
      date: (value: DateInput) => guard(value, (d) => format.dateTime(d, { dateStyle: "medium" })),
      /** 14:03:21 */
      time: (value: DateInput) =>
        guard(value, (d) =>
          format.dateTime(d, { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
        ),
      /** "3 dakika önce" / "3 minutes ago" */
      relative: (value: DateInput) => guard(value, (d) => format.relativeTime(d, now)),
      number: (value: number) => format.number(value),
    };
  }, [format, now]);
}
