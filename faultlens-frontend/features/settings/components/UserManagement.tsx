"use client";

import { useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { KeyRound, Trash2, UserPlus } from "lucide-react";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { z } from "zod";
import type { Role } from "@/features/auth/hooks/useCan";
import { useAuthStore } from "@/features/auth/store/auth.store";
import {
  useChangeRoleMutation,
  useCreateUserMutation,
  useDeleteUserMutation,
  useResetPasswordMutation,
  useUsersQuery,
} from "@/features/users/api/useUsers";
import type { ManagedUser } from "@/features/users/types/user.types";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { Modal } from "@/shared/components/ui/Modal";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { useValidationMessage } from "@/shared/hooks/useValidationMessage";

const ROLES: Role[] = ["ADMIN", "EDITOR", "VIEWER"];

/** Messages are keys of the "validation" namespace. */
export const createUserSchema = z.object({
  username: z.string().trim().min(1, "usernameRequired").max(100),
  email: z.union([z.literal(""), z.email("emailInvalid")]),
  role: z.enum(["ADMIN", "EDITOR", "VIEWER"]),
  password: z.string().min(8, "passwordMin").max(128),
});
type CreateUserForm = z.infer<typeof createUserSchema>;

const EMPTY_FORM: CreateUserForm = { username: "", email: "", role: "VIEWER", password: "" };

const ROLE_BADGE: Record<Role, "error" | "warning" | "info"> = {
  ADMIN: "error",
  EDITOR: "warning",
  VIEWER: "info",
};

/** Temporary password for another user (replaces the old SweetAlert prompt). */
function ResetPasswordDialog({
  user,
  onClose,
  onSubmit,
  busy,
}: {
  user: ManagedUser | null;
  onClose: () => void;
  onSubmit: (password: string) => void;
  busy: boolean;
}) {
  const t = useTranslations("settings.users");
  const v = useValidationMessage();
  const tCommon = useTranslations("common");
  const [password, setPassword] = useState("");
  const [touched, setTouched] = useState(false);
  const invalid = password.length < 8;

  const close = () => {
    setPassword("");
    setTouched(false);
    onClose();
  };

  return (
    <Modal
      isOpen={user !== null}
      onClose={close}
      title={user ? t("resetTitle", { user: user.username }) : ""}
      description={t("resetDescription")}
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={close}>
            {tCommon("cancel")}
          </Button>
          <Button type="submit" form="reset-password-form" variant="primary" loading={busy}>
            {t("resetConfirm")}
          </Button>
        </>
      }
    >
      <form
        id="reset-password-form"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          setTouched(true);
          if (invalid) return;
          onSubmit(password);
          close();
        }}
      >
        <Input
          label={t("tempPassword")}
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={touched && invalid ? v("passwordMin") : undefined}
          autoFocus
        />
      </form>
    </Modal>
  );
}

export function UserManagement() {
  const t = useTranslations("settings.users");
  const v = useValidationMessage();
  const currentUsername = useAuthStore((state) => state.user?.username);
  const { data: users, isLoading } = useUsersQuery();
  const createUser = useCreateUserMutation();
  const changeRole = useChangeRoleMutation();
  const resetPassword = useResetPasswordMutation();
  const deleteUser = useDeleteUserMutation();
  const { confirm, dialog } = useConfirm();
  const [resetTarget, setResetTarget] = useState<ManagedUser | null>(null);
  const { mutate: setRole } = changeRole;
  const isMutating = changeRole.isPending || resetPassword.isPending || deleteUser.isPending;

  const {
    register,
    handleSubmit,
    reset: resetForm,
    formState: { errors },
  } = useForm<CreateUserForm>({
    resolver: zodResolver(createUserSchema),
    defaultValues: EMPTY_FORM,
  });

  const onCreate = (form: CreateUserForm) => {
    createUser.mutate(
      { ...form, email: form.email || undefined },
      { onSuccess: () => resetForm(EMPTY_FORM) },
    );
  };

  const columns = useMemo<DataTableColumnDef<ManagedUser>[]>(
    () => [
      {
        accessorKey: "username",
        header: t("columns.user"),
        cell: ({ row }) => (
          <div className="min-w-[8rem]">
            <div className="font-medium text-text-primary">
              {row.original.username}
              {row.original.username === currentUsername && (
                <span className="ml-2 xs t3">{t("you")}</span>
              )}
            </div>
            {row.original.email && <div className="xs t3">{row.original.email}</div>}
          </div>
        ),
      },
      {
        accessorKey: "role",
        header: t("columns.role"),
        cell: ({ row }) => {
          const user = row.original;
          if (user.username === currentUsername) {
            return (
              <Badge variant={ROLE_BADGE[user.role]} mono>
                {user.role}
              </Badge>
            );
          }
          return (
            <select
              aria-label={t("roleOf", { user: user.username })}
              className="input w-auto h-[30px]"
              value={user.role}
              disabled={isMutating}
              onChange={(e) => setRole({ id: user.id, role: e.target.value as Role })}
            >
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {t(`roles.${role}`)}
                </option>
              ))}
            </select>
          );
        },
      },
      {
        accessorKey: "mustChangePassword",
        header: t("columns.status"),
        cell: ({ row }) =>
          row.original.mustChangePassword ? (
            <Badge variant="warning" dot>
              {t("pendingPassword")}
            </Badge>
          ) : (
            <Badge variant="success" dot>
              {t("active")}
            </Badge>
          ),
      },
      {
        id: "actions",
        header: t("columns.actions"),
        cell: ({ row }) => {
          const user = row.original;
          if (user.username === currentUsername) return null;
          return (
            <div className="flex items-center gap-1 whitespace-nowrap">
              <Button
                variant="ghost"
                size="sm"
                icon={<KeyRound size={14} aria-hidden="true" />}
                onClick={() => setResetTarget(user)}
                disabled={isMutating}
                title={t("resetPassword")}
                aria-label={t("resetPassword")}
              />
              <Button
                variant="ghost"
                size="sm"
                className="text-error"
                icon={<Trash2 size={14} aria-hidden="true" />}
                onClick={() =>
                  confirm({
                    title: t("deleteTitle", { user: user.username }),
                    description: t("deleteDescription"),
                    confirmLabel: t("deleteConfirm"),
                    onConfirm: () => deleteUser.mutateAsync(user.id),
                  })
                }
                disabled={isMutating}
                title={t("delete")}
                aria-label={t("delete")}
              />
            </div>
          );
        },
      },
    ],
    [t, currentUsername, isMutating, setRole, confirm, deleteUser],
  );

  return (
    <div className="stack gap-6">
      <header>
        <h2 className="h2">{t("title")}</h2>
        <p className="small t3 mt-1">{t("subtitle")}</p>
      </header>

      <DataTable
        label={t("title")}
        data={users ?? []}
        columns={columns}
        loading={isLoading}
        emptyMessage={t("empty")}
      />

      <form onSubmit={handleSubmit(onCreate)} className="card p-4 stack gap-4" noValidate>
        <h3 className="h3">{t("newUser")}</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            label={t("username")}
            autoComplete="off"
            error={v(errors.username?.message)}
            {...register("username")}
          />
          <Input
            label={t("email")}
            id="new-user-email"
            type="email"
            error={v(errors.email?.message)}
            {...register("email")}
          />
          <Input
            label={t("tempPassword")}
            id="new-user-password"
            type="password"
            autoComplete="new-password"
            hint={t("tempPasswordHint")}
            error={v(errors.password?.message)}
            {...register("password")}
          />
          <div className="field">
            <label htmlFor="new-user-role" className="label">
              {t("role")}
            </label>
            <select id="new-user-role" className="input" {...register("role")}>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {t(`roles.${role}`)} — {t(`roleDescriptions.${role}`)}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div>
          <Button
            type="submit"
            icon={<UserPlus size={16} aria-hidden="true" />}
            loading={createUser.isPending}
          >
            {t("create")}
          </Button>
        </div>
      </form>

      <ResetPasswordDialog
        user={resetTarget}
        onClose={() => setResetTarget(null)}
        busy={resetPassword.isPending}
        onSubmit={(password) =>
          resetTarget && resetPassword.mutate({ id: resetTarget.id, password })
        }
      />
      {dialog}
    </div>
  );
}
