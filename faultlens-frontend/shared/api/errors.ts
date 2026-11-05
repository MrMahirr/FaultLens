/**
 * Extracts a user-facing message from an API error.
 *
 * The backend answers with two envelope shapes:
 * - ErrorEnvelope: `{ success: false, error: { code, message } }` (exceptions, validation)
 * - ApiResponse.error: `{ success: false, message }` (a few controllers)
 * Reading only one of them made several screens fall back to a generic text.
 */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  const data = (
    error as { response?: { data?: { error?: { message?: unknown }; message?: unknown } } }
  )?.response?.data;
  const message = data?.error?.message ?? data?.message;
  return typeof message === "string" && message.trim() !== "" ? message : fallback;
}

/** Returns the backend error code (`error.code`), if any. */
export function getApiErrorCode(error: unknown): string | undefined {
  const code = (error as { response?: { data?: { error?: { code?: unknown } } } })?.response?.data
    ?.error?.code;
  return typeof code === "string" ? code : undefined;
}
