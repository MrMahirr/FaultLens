"use client";

import { useTranslations } from "next-intl";
import { RotateCcw } from "lucide-react";
import { Badge, type BadgeVariant } from "@/shared/components/ui/Badge";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { GroupStatus, type LogGroupDto } from "@/features/logs/types/log.types";

const VARIANT: Record<GroupStatus, BadgeVariant> = {
  [GroupStatus.UNRESOLVED]: "error",
  [GroupStatus.RESOLVED]: "success",
  [GroupStatus.IGNORED]: "outline",
};

/** Status (with a dot, never colour alone), the ignore limit, and a regression marker. */
export function GroupStatusBadge({
  group,
}: {
  group: Pick<LogGroupDto, "status" | "ignoreUntil" | "regressionCount">;
}) {
  const t = useTranslations("logs.groups");
  const dates = useDateFormat();
  // Unresolved again after a resolution: the case that needs attention first.
  const regressed = group.status === GroupStatus.UNRESOLVED && group.regressionCount > 0;

  return (
    <span className="row gap-1 flex-wrap">
      <Badge variant={VARIANT[group.status]} dot>
        {t(`status.${group.status}`)}
      </Badge>
      {regressed && (
        <span title={t("regressionHint")}>
          <Badge variant="warning" icon={<RotateCcw size={11} aria-hidden="true" />}>
            {t("regressionBadge", { count: group.regressionCount })}
          </Badge>
        </span>
      )}
      {group.status === GroupStatus.IGNORED && group.ignoreUntil && (
        <span className="xs t3">
          {t("ignoredUntil", { time: dates.dateTime(group.ignoreUntil) })}
        </span>
      )}
    </span>
  );
}
