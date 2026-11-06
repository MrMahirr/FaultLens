import type { Locale } from "@/i18n/config";

import trCommon from "./tr/common.json";
import trShell from "./tr/shell.json";
import trAlarms from "./tr/alarms.json";

import enCommon from "./en/common.json";
import enShell from "./en/shell.json";
import enAlarms from "./en/alarms.json";

/**
 * UI messages per locale, one JSON file per area (messages/<locale>/<area>.json); each file
 * holds one or more top-level namespaces. Turkish is the reference: `Messages` is its type,
 * so tsc rejects unknown keys, and messages.test.ts keeps the English tree identical.
 */
const tr = { ...trCommon, ...trShell, ...trAlarms };
const en = { ...enCommon, ...enShell, ...enAlarms };

export type Messages = typeof tr;

export const MESSAGES: Record<Locale, Messages> = { tr, en };
