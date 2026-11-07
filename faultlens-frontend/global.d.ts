import type { Locale } from "@/i18n/config";
import type { Messages } from "@/messages";

// Typed message keys: a missing or misspelt key fails `tsc`.
declare module "next-intl" {
  interface AppConfig {
    Locale: Locale;
    Messages: Messages;
  }
}
