"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { MessageSquare } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { getApiErrorMessage } from "@/shared/api/errors";
import { useCan } from "@/features/auth/hooks/useCan";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useLogGroupCommentsQuery } from "@/features/logs/api/useLogQuery";
import {
  useAddCommentMutation,
  useDeleteCommentMutation,
  useEditCommentMutation,
} from "@/features/logs/api/useLogMutation";
import type { LogGroupCommentDto } from "@/features/logs/types/log.types";

const MAX_LENGTH = 10_000;

/**
 * Notes on a group, oldest first. Bodies are plain text rendered as text (whitespace kept), so a
 * note can never inject HTML. Editors write; the author edits; the author or an admin deletes.
 */
export function GroupComments({ groupId }: { groupId: number }) {
  const t = useTranslations("logs.groups.comments");
  const dates = useDateFormat();
  const canEdit = useCan("edit");
  const isAdmin = useCan("manageUsers");
  const me = useAuthStore((state) => state.user?.username);
  const { data: comments, isLoading } = useLogGroupCommentsQuery(groupId);
  const add = useAddCommentMutation();
  const edit = useEditCommentMutation();
  const remove = useDeleteCommentMutation();
  const { confirm, dialog } = useConfirm();
  const [draft, setDraft] = useState("");
  const [editing, setEditing] = useState<{ id: number; body: string } | null>(null);
  const inputId = useId();

  const submit = () => {
    const body = draft.trim();
    if (!body) return;
    add.mutate(
      { id: groupId, body },
      {
        onSuccess: () => setDraft(""),
        onError: (error) => toast.error(getApiErrorMessage(error, t("failed"))),
      },
    );
  };

  const saveEdit = () => {
    if (!editing || !editing.body.trim()) return;
    edit.mutate(
      { id: groupId, commentId: editing.id, body: editing.body.trim() },
      {
        onSuccess: () => setEditing(null),
        onError: (error) => toast.error(getApiErrorMessage(error, t("failed"))),
      },
    );
  };

  const askDelete = (comment: LogGroupCommentDto) =>
    confirm({
      title: t("deleteTitle"),
      description: t("deleteDescription"),
      confirmLabel: t("delete"),
      onConfirm: () => remove.mutateAsync({ id: groupId, commentId: comment.id }),
    });

  return (
    <section className="card p-4 stack gap-3" aria-labelledby="group-comments">
      <h2 id="group-comments" className="h3 row gap-2">
        <MessageSquare size={15} className="t3" aria-hidden="true" />
        {t("title")}
      </h2>

      {!isLoading && (comments ?? []).length === 0 && <p className="small t3">{t("empty")}</p>}

      <ol className="m-0 p-0 list-none stack gap-3">
        {(comments ?? []).map((comment) => {
          const mine = comment.author === me;
          const isEditing = editing?.id === comment.id;
          return (
            <li key={comment.id} className="stack gap-1">
              <p className="xs t3 m-0 row gap-1">
                <span className="font-medium t2">{comment.author}</span>·
                <time dateTime={comment.createdAt} title={dates.dateTime(comment.createdAt)}>
                  {dates.relative(comment.createdAt)}
                </time>
                {comment.editedAt && <span>· {t("edited")}</span>}
              </p>
              {isEditing ? (
                <div className="stack gap-2">
                  <textarea
                    className="input min-h-[80px] py-2"
                    aria-label={t("edit")}
                    maxLength={MAX_LENGTH}
                    value={editing.body}
                    onChange={(e) => setEditing({ id: comment.id, body: e.target.value })}
                    autoFocus
                  />
                  <div className="row gap-1">
                    <Button size="sm" variant="primary" loading={edit.isPending} onClick={saveEdit}>
                      {t("save")}
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setEditing(null)}>
                      {t("cancel")}
                    </Button>
                  </div>
                </div>
              ) : (
                <p className="small m-0 whitespace-pre-wrap break-words">{comment.body}</p>
              )}
              {!isEditing && canEdit && (mine || isAdmin) && (
                <div className="row gap-1">
                  {mine && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setEditing({ id: comment.id, body: comment.body })}
                    >
                      {t("edit")}
                    </Button>
                  )}
                  <Button size="sm" variant="ghost" onClick={() => askDelete(comment)}>
                    {t("delete")}
                  </Button>
                </div>
              )}
            </li>
          );
        })}
      </ol>

      {canEdit && (
        <form
          className="stack gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            submit();
          }}
        >
          <label htmlFor={inputId} className="sr-only">
            {t("label")}
          </label>
          <textarea
            id={inputId}
            className="input min-h-[80px] py-2"
            placeholder={t("placeholder")}
            maxLength={MAX_LENGTH}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              // Ctrl/⌘+Enter sends, like most issue trackers.
              if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
                e.preventDefault();
                submit();
              }
            }}
          />
          <Button
            type="submit"
            size="sm"
            variant="primary"
            className="w-fit"
            loading={add.isPending}
            disabled={!draft.trim()}
          >
            {t("add")}
          </Button>
        </form>
      )}
      {dialog}
    </section>
  );
}
