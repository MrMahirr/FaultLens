"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { Layers, ScrollText } from "lucide-react";

/** Switches between the entry list (/logs) and the error groups (/logs/groups). */
export function LogsNav() {
  const t = useTranslations("logs.nav");
  const pathname = usePathname();
  const onGroups = pathname.startsWith("/logs/groups");

  return (
    <nav aria-label={t("label")} className="tabs">
      <Link href="/logs" className="tab" aria-current={onGroups ? undefined : "page"}>
        <ScrollText size={14} aria-hidden="true" />
        {t("entries")}
      </Link>
      <Link href="/logs/groups" className="tab" aria-current={onGroups ? "page" : undefined}>
        <Layers size={14} aria-hidden="true" />
        {t("groups")}
      </Link>
    </nav>
  );
}
