/**
 * Types of the API contract (P10 / 1.3), generated from contracts/openapi.json by
 * `pnpm gen:api` into ./schema.d.ts. Do not edit schema.d.ts by hand.
 *
 * The wire format is snake_case; the axios interceptors convert every key to camelCase
 * (shared/lib/caseTransform.ts). `ApiSchema<"LogEntryDto">` is a schema as the app sees it,
 * after that conversion.
 */
import type { components } from "./schema";

export type Schemas = components["schemas"];
export type SchemaName = keyof Schemas;

/**
 * Type-level twin of caseTransform's snakeToCamelKey: `_` + lowercase letter becomes the
 * uppercase letter, everything else is kept ("source_id" → "sourceId", "TRACE" → "TRACE").
 * (type-fest's CamelCase would turn "TRACE" into "trace", which the runtime does not do.)
 */
export type SnakeToCamel<S extends string> = S extends `${infer Head}_${infer C}${infer Rest}`
  ? C extends Lowercase<C>
    ? C extends Uppercase<C>
      ? `${Head}_${SnakeToCamel<`${C}${Rest}`>}` // "_1", "__": not a letter, kept
      : `${Head}${Uppercase<C>}${SnakeToCamel<Rest>}`
    : `${Head}_${SnakeToCamel<`${C}${Rest}`>}`
  : S;

/** Deep camelCase keys, like snakeToCamel() does at runtime. */
export type CamelKeys<T> = T extends readonly (infer Item)[]
  ? CamelKeys<Item>[]
  : T extends object
    ? { [K in keyof T as K extends string ? SnakeToCamel<K> : K]: CamelKeys<T[K]> }
    : T;

/** A contract schema with camelCase keys, e.g. `ApiSchema<"AlarmDto">["triggeredAt"]`. */
export type ApiSchema<N extends SchemaName> = CamelKeys<Schemas[N]>;

/** Value type of one schema field, without `undefined` (generated fields are optional). */
export type ApiField<N extends SchemaName, K extends keyof ApiSchema<N>> = NonNullable<
  ApiSchema<N>[K]
>;
