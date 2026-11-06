import type { Locale } from "@/i18n/config";

import trCommon from "./tr/common.json";
import trShell from "./tr/shell.json";
import trAlarms from "./tr/alarms.json";
import trLogs from "./tr/logs.json";
import trAnalyses from "./tr/analyses.json";
import trDashboard from "./tr/dashboard.json";
import trSources from "./tr/sources.json";
import trDeployments from "./tr/deployments.json";
import trSettings from "./tr/settings.json";
import trAuth from "./tr/auth.json";

import enCommon from "./en/common.json";
import enShell from "./en/shell.json";
import enAlarms from "./en/alarms.json";
import enLogs from "./en/logs.json";
import enAnalyses from "./en/analyses.json";
import enDashboard from "./en/dashboard.json";
import enSources from "./en/sources.json";
import enDeployments from "./en/deployments.json";
import enSettings from "./en/settings.json";
import enAuth from "./en/auth.json";

/**
 * UI messages per locale, one JSON file per area (messages/<locale>/<area>.json); each file
 * holds one or more top-level namespaces. Turkish is the reference: `Messages` is its type,
 * so tsc rejects unknown keys, and messages.test.ts keeps the English tree identical.
 */
const tr = {
  ...trCommon,
  ...trShell,
  ...trAlarms,
  ...trLogs,
  ...trAnalyses,
  ...trDashboard,
  ...trSources,
  ...trDeployments,
  ...trSettings,
  ...trAuth,
};
const en = {
  ...enCommon,
  ...enShell,
  ...enAlarms,
  ...enLogs,
  ...enAnalyses,
  ...enDashboard,
  ...enSources,
  ...enDeployments,
  ...enSettings,
  ...enAuth,
};

export type Messages = typeof tr;

export const MESSAGES: Record<Locale, Messages> = { tr, en };
