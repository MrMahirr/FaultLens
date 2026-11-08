"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { GitMerge } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { Modal } from "@/shared/components/ui/Modal";
import { Input } from "@/shared/components/ui/Input";
import { DropdownSelect } from "@/shared/components/ui/Dropdown";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { getApiErrorMessage } from "@/shared/api/errors";
import { useCan } from "@/features/auth/hooks/useCan";
import { useLogGroupsQuery } from "@/features/logs/api/useLogQuery";
import { useMergeGroupMutation, useUnmergeGroupMutation } from "@/features/logs/api/useLogMutation";
import { GroupStatus, type LogGroupDto } from "@/features/logs/types/log.types";

const ALL_STATUSES = Object.values(GroupStatus);

function shorten(text: string, max = 70) {
  return text.length > max ? `${text.slice(0, max)}…` : text;
}

/**
 * "Merge" button + dialog (editors). Merging is undone only by admins, so only they get the
 * toast's undo.
 */
export function MergeGroupButton({ group }: { group: Pick<LogGroupDto, "id"> }) {
  const t = useTranslations("logs.groups.merge");
  const isAdmin = useCan("manageUsers");
  const [open, setOpen] = useState(false);
  const [target, setTarget] = useState("");
  // Merged groups are not listed by the API, so every option is a valid target.
  // Fetched only while the dialog is open, without polling. Older groups: type their number.
  const { data } = useLogGroupsQuery(
    { statuses: ALL_STATUSES, size: 100 },
    { enabled: open, poll: false },
  );
  const merge = useMergeGroupMutation();
  const unmerge = useUnmergeGroupMutation();

  const options = (data?.content ?? [])
    .filter((candidate) => candidate.id !== group.id)
    .map((candidate) => ({
      label: t("targetOption", { id: candidate.id, message: shorten(candidate.firstMessage) }),
      value: String(candidate.id),
    }));

  const submit = async () => {
    const targetId = Number(target);
    try {
      await merge.mutateAsync({ id: group.id, targetId });
      setOpen(false);
      setTarget("");
      if (!isAdmin) {
        toast.success(t("merged", { id: targetId }));
        return;
      }
      toast.success(
        (item) => (
          <span className="row gap-3">
            {t("merged", { id: targetId })}
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => {
                toast.dismiss(item.id);
                unmerge.mutate(
                  { id: group.id },
                  { onError: (error) => toast.error(getApiErrorMessage(error, t("failed"))) },
                );
              }}
            >
              {t("undo")}
            </button>
          </span>
        ),
        { duration: 6000 },
      );
    } catch (error) {
      toast.error(getApiErrorMessage(error, t("failed")));
    }
  };

  return (
    <>
      <Button
        size="md"
        variant="ghost"
        icon={<GitMerge size={13} aria-hidden="true" />}
        onClick={() => setOpen(true)}
      >
        {t("action")}
      </Button>
      <Modal
        isOpen={open}
        onClose={() => setOpen(false)}
        title={t("title")}
        description={t("description")}
        footer={
          <Button
            variant="primary"
            loading={merge.isPending}
            disabled={!target || Number(target) === group.id}
            onClick={() => void submit()}
          >
            {t("confirm")}
          </Button>
        }
      >
        <div className="stack gap-3">
          <DropdownSelect
            label={t("target")}
            value={options.some((option) => option.value === target) ? target : ""}
            options={options}
            onChange={setTarget}
            placeholder={t("targetPlaceholder")}
          />
          {/* The list holds the 100 most recently seen groups; older ones by number. */}
          <Input
            label={t("targetId")}
            inputMode="numeric"
            className="mono"
            value={target}
            onChange={(e) => setTarget(e.target.value.replace(/\D/g, ""))}
          />
        </div>
      </Modal>
    </>
  );
}

/** Shown on a merged group: where its entries go now, and "unmerge" for admins. */
export function MergedBanner({ group }: { group: Pick<LogGroupDto, "id" | "mergedIntoId"> }) {
  const t = useTranslations("logs.groups.merge");
  const isAdmin = useCan("manageUsers");
  const unmerge = useUnmergeGroupMutation();
  const { confirm, dialog } = useConfirm();
  if (!group.mergedIntoId) return null;

  return (
    <div className="card p-3 row gap-3 flex-wrap c-info" role="status">
      <GitMerge size={15} aria-hidden="true" />
      <span className="small grow">{t("banner", { id: group.mergedIntoId })}</span>
      <Link href={`/logs/groups/${group.mergedIntoId}`} className="btn btn-sm">
        {t("openTarget")}
      </Link>
      {isAdmin && (
        <Button
          size="sm"
          variant="ghost"
          onClick={() =>
            confirm({
              title: t("unmergeTitle"),
              description: t("unmergeDescription"),
              confirmLabel: t("unmerge"),
              destructive: false,
              onConfirm: () =>
                unmerge
                  .mutateAsync({ id: group.id })
                  .then(() => toast.success(t("unmerged")))
                  .catch((error: unknown) => toast.error(getApiErrorMessage(error, t("failed")))),
            })
          }
        >
          {t("unmerge")}
        </Button>
      )}
      {dialog}
    </div>
  );
}
