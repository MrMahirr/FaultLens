/**
 * Deep key-case transformation utilities.
 *
 * Backend tüm JSON field'ları snake_case olarak döndürüyor
 * (@JsonNaming(SnakeCaseStrategy.class)).
 * Frontend ise TypeScript convention'ına uygun olarak camelCase kullanıyor.
 *
 * Bu modül, iki taraf arasındaki naming uyumsuzluğunu
 * tek bir noktada (Axios interceptor) çözer.
 */

/* ── Primitive Key Converters ────────────────────────────────── */

/**
 * Converts a single snake_case key to camelCase.
 * @example "source_id" → "sourceId"
 * @example "first_seen_at" → "firstSeenAt"
 */
function snakeToCamelKey(key: string): string {
  return key.replace(/_([a-z])/g, (_, char) => char.toUpperCase());
}

/**
 * Converts a single camelCase key to snake_case.
 * @example "sourceId" → "source_id"
 * @example "firstSeenAt" → "first_seen_at"
 */
function camelToSnakeKey(key: string): string {
  return key.replace(/[A-Z]/g, (char) => `_${char.toLowerCase()}`);
}

/* ── Deep Object Transformers ────────────────────────────────── */

/**
 * Recursively transforms all keys of an object from snake_case to camelCase.
 * Handles nested objects and arrays. Returns primitives as-is.
 */
export function snakeToCamel<T = unknown>(data: unknown): T {
  if (data === null || data === undefined) {
    return data as T;
  }

  if (Array.isArray(data)) {
    return data.map((item) => snakeToCamel(item)) as T;
  }

  if (typeof data === "object" && !(data instanceof Date)) {
    const result: Record<string, unknown> = {};

    for (const [key, value] of Object.entries(data as Record<string, unknown>)) {
      result[snakeToCamelKey(key)] = snakeToCamel(value);
    }

    return result as T;
  }

  return data as T;
}

/**
 * Recursively transforms all keys of an object from camelCase to snake_case.
 * Handles nested objects and arrays. Returns primitives as-is.
 */
export function camelToSnake<T = unknown>(data: unknown): T {
  if (data === null || data === undefined) {
    return data as T;
  }

  if (Array.isArray(data)) {
    return data.map((item) => camelToSnake(item)) as T;
  }

  if (typeof data === "object" && !(data instanceof Date)) {
    const result: Record<string, unknown> = {};

    for (const [key, value] of Object.entries(data as Record<string, unknown>)) {
      result[camelToSnakeKey(key)] = camelToSnake(value);
    }

    return result as T;
  }

  return data as T;
}
