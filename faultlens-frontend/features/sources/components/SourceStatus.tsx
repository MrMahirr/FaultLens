"use client";

import { useTranslations } from "next-intl";
import { ConnectionStatus } from "@/shared/types/common.types";
import { cn } from "@/shared/lib/utils";

const TONE: Record<ConnectionStatus, string> = {
  [ConnectionStatus.CONNECTED]: "c-ok",
  [ConnectionStatus.WARNING]: "c-warn",
  [ConnectionStatus.DISCONNECTED]: "c-crit",
};

/** Connection status as dot + text (the dot pings while connected). */
export function SourceStatus({ status }: { status?: ConnectionStatus }) {
  const t = useTranslations("sources.status");
  const value = status ?? ConnectionStatus.DISCONNECTED;
  return (
    <span className={cn("badge", TONE[value])}>
      <span
        className={cn("dot w-1.5 h-1.5", value === ConnectionStatus.CONNECTED && "live")}
        style={{ background: "currentColor" }}
        aria-hidden="true"
      />
      {t(value)}
    </span>
  );
}
