"use client";

import { ErrorState } from "@/shared/components/feedback/ErrorState";

export interface RouteErrorProps {
  /** Whatever was thrown (Client Components may throw non-Error values). */
  error: unknown;
  /** Re-fetches and re-renders the segment (Next 16.2+). */
  retry?: () => void;
  /** Re-renders without re-fetching. */
  reset?: () => void;
}

/** Shared body of every route's error.tsx: banner + retry, the shell stays usable. */
export function RouteError({ error, retry, reset }: RouteErrorProps) {
  const err = error instanceof Error ? (error as Error & { digest?: string }) : null;
  return <ErrorState error={err} onRetry={retry ?? reset} />;
}
