"use client";

import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { BellOff, CheckCircle2, ChevronDown, RotateCcw } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/shared/components/ui/Button";
import { Dropdown } from "@/shared/components/ui/Dropdown";
import { getApiErrorCode, getApiErrorMessage } from "@/shared/api/errors";
import { useUpdateGroupStatusMutation } from "@/features/logs/api/useLogMutation";
import { logKeys } from "@/features/logs/api/useLogQuery";
import {
  GroupStatus,
  type GroupStatusUpdate,
  type LogGroupDto,
} from "@/features/logs/types/log.types";

const HOUR = 60 * 60 * 1000;
/** Ignore periods offered in the menu; "forever" = until someone reopens it. */
export const IGNORE_PERIODS = { "1h": HOUR, "24h": 24 * HOUR, "7d": 7 * 24 * HOUR, forever: 0 };
type IgnorePeriod = keyof typeof IGNORE_PERIODS;

/** The change that puts a group back where it was (for the toast's undo). */
export function undoUpdate(
  previous: Pick<LogGroupDto, "status" | "ignoreUntil">,
  now = Date.now(),
) {
  const update: GroupStatusUpdate = { status: previous.status };
  if (
    previous.status === GroupStatus.IGNORED &&
    previous.ignoreUntil &&
    new Date(previous.ignoreUntil).getTime() > now
  ) {
    update.ignoreUntil = previous.ignoreUntil;
  }
  return update;
}

/**
 * Resolve / ignore (with a period) / reopen. Every change is reversible, so there is no
 * confirmation; the toast offers undo instead. Callers hide this for viewers (useCan("edit")).
 */
export function GroupActions({
  group,
  size = "sm",
}: {
  group: Pick<LogGroupDto, "id" | "status" | "ignoreUntil">;
  size?: "sm" | "md";
}) {
  const t = useTranslations("logs.groups.actions");
  const queryClient = useQueryClient();
  const mutation = useUpdateGroupStatusMutation();

  const apply = (update: GroupStatusUpdate, undoable: boolean) => {
    const previous = { status: group.status, ignoreUntil: group.ignoreUntil };
    mutation.mutate(
      { id: group.id, update },
      {
        onSuccess: () => {
          const message =
            update.status === GroupStatus.RESOLVED
              ? t("resolved")
              : update.status === GroupStatus.IGNORED
                ? t("ignored")
                : t("reopened");
          if (!undoable) {
            toast.success(message);
            return;
          }
          toast.success(
            (toastItem) => (
              <span className="row gap-3">
                {message}
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => {
                    toast.dismiss(toastItem.id);
                    apply(undoUpdate(previous), false);
                  }}
                >
                  {t("undo")}
                </button>
              </span>
            ),
            { duration: 6000 },
          );
        },
        onError: (error) => {
          if (getApiErrorCode(error) === "GROUP_STATUS_CONFLICT") {
            toast.error(t("conflict"));
            void queryClient.invalidateQueries({ queryKey: logKeys.groups() });
          } else {
            toast.error(getApiErrorMessage(error, t("failed")));
          }
        },
      },
    );
  };

  const ignore = (period: IgnorePeriod) => {
    const ms = IGNORE_PERIODS[period];
    apply(
      {
        status: GroupStatus.IGNORED,
        ...(ms ? { ignoreUntil: new Date(Date.now() + ms).toISOString() } : {}),
      },
      true,
    );
  };

  const ignoreItems = (Object.keys(IGNORE_PERIODS) as IgnorePeriod[]).map((period) => ({
    label: t(`ignoreFor.${period}`),
    value: period,
  }));

  return (
    <div className="row gap-1" onClick={(e) => e.stopPropagation()}>
      {group.status === GroupStatus.RESOLVED ? (
        <Button
          size={size}
          variant="secondary"
          icon={<RotateCcw size={13} aria-hidden="true" />}
          loading={mutation.isPending}
          onClick={() => apply({ status: GroupStatus.UNRESOLVED }, true)}
        >
          {t("reopen")}
        </Button>
      ) : (
        <Button
          size={size}
          variant="secondary"
          icon={<CheckCircle2 size={13} aria-hidden="true" />}
          loading={mutation.isPending}
          onClick={() => apply({ status: GroupStatus.RESOLVED }, true)}
        >
          {t("resolve")}
        </Button>
      )}
      {group.status === GroupStatus.IGNORED ? (
        <Button
          size={size}
          variant="ghost"
          icon={<RotateCcw size={13} aria-hidden="true" />}
          disabled={mutation.isPending}
          onClick={() => apply({ status: GroupStatus.UNRESOLVED }, true)}
        >
          {t("reopen")}
        </Button>
      ) : (
        <Dropdown
          label={t("ignore")}
          items={ignoreItems}
          onSelect={(value) => ignore(value as IgnorePeriod)}
          trigger={
            <Button
              size={size}
              variant="ghost"
              icon={<BellOff size={13} aria-hidden="true" />}
              disabled={mutation.isPending}
            >
              {t("ignore")}
              <ChevronDown size={12} aria-hidden="true" />
            </Button>
          }
        />
      )}
    </div>
  );
}
