"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { UserRound } from "lucide-react";
import { DropdownSelect } from "@/shared/components/ui/Dropdown";
import { getApiErrorMessage } from "@/shared/api/errors";
import { useCan } from "@/features/auth/hooks/useCan";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useAssignableUsersQuery } from "@/features/logs/api/useLogQuery";
import { useAssignGroupMutation } from "@/features/logs/api/useLogMutation";
import type { LogGroupDto } from "@/features/logs/types/log.types";

/** Editors pick the assignee; viewers see who it is. The assignee is notified in-app. */
export function GroupAssignee({ group }: { group: Pick<LogGroupDto, "id" | "assignee"> }) {
  const t = useTranslations("logs.groups.assign");
  const canEdit = useCan("edit");
  const me = useAuthStore((state) => state.user?.username);
  const { data: users } = useAssignableUsersQuery(canEdit);
  const assign = useAssignGroupMutation();

  if (!canEdit) {
    return (
      <div className="field">
        <span className="eyebrow">{t("label")}</span>
        <span className="row gap-1 small">
          <UserRound size={13} aria-hidden="true" />
          {group.assignee ?? t("none")}
        </span>
      </div>
    );
  }

  const names = new Set((users ?? []).map((user) => user.username));
  // The current assignee stays selectable even if the list is not loaded (or they were deleted).
  if (group.assignee) names.add(group.assignee);
  const options = [
    { label: t("none"), value: "" },
    ...[...names].sort().map((name) => ({
      label: name === me ? t("me", { user: name }) : name,
      value: name,
    })),
  ];

  return (
    <DropdownSelect
      label={t("label")}
      value={group.assignee ?? ""}
      options={options}
      className="sm:w-56"
      onChange={(value) => {
        const assignee = value || null;
        if (assignee === group.assignee) return;
        assign.mutate(
          { id: group.id, assignee },
          {
            onSuccess: () =>
              toast.success(assignee ? t("assigned", { user: assignee }) : t("unassigned")),
            onError: (error) => toast.error(getApiErrorMessage(error, t("failed"))),
          },
        );
      }}
    />
  );
}
