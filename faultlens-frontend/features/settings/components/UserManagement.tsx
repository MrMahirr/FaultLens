"use client";

import { useCallback, useMemo } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { KeyRound, Trash2, UserPlus } from "lucide-react";
import { useForm } from "react-hook-form";
import Swal from "sweetalert2";
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
import {
  ROLE_DESCRIPTIONS,
  ROLE_LABELS,
  type ManagedUser,
} from "@/features/users/types/user.types";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";

const ROLES: Role[] = ["ADMIN", "EDITOR", "VIEWER"];

export const createUserSchema = z.object({
  username: z.string().trim().min(1, "Kullanıcı adı gereklidir").max(100),
  email: z.union([z.literal(""), z.email("Geçersiz e-posta")]),
  role: z.enum(["ADMIN", "EDITOR", "VIEWER"]),
  password: z.string().min(8, "Şifre en az 8 karakter olmalı").max(128),
});
type CreateUserForm = z.infer<typeof createUserSchema>;

const EMPTY_FORM: CreateUserForm = { username: "", email: "", role: "VIEWER", password: "" };

const selectClass =
  "bg-bg-tertiary text-text-primary rounded-lg border border-border-default px-3 py-2 text-sm focus:outline-none focus:border-border-active focus:ring-1 focus:ring-accent/30 cursor-pointer disabled:cursor-not-allowed disabled:opacity-60";

function roleVariant(role: Role): "error" | "warning" | "info" {
  if (role === "ADMIN") return "error";
  if (role === "EDITOR") return "warning";
  return "info";
}

export function UserManagement() {
  const currentUsername = useAuthStore((state) => state.user?.username);
  const { data: users, isLoading } = useUsersQuery();
  const createUser = useCreateUserMutation();
  const changeRole = useChangeRoleMutation();
  const resetPassword = useResetPasswordMutation();
  const deleteUser = useDeleteUserMutation();
  const { mutate: setRole } = changeRole;
  const { mutate: reset } = resetPassword;
  const { mutate: remove } = deleteUser;
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

  const confirmDelete = useCallback(
    async (user: ManagedUser) => {
      const result = await Swal.fire({
        title: "Kullanıcı silinsin mi?",
        text: `"${user.username}" ve bildirim ayarları kalıcı olarak silinir.`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#ef4444",
        cancelButtonColor: "#3b82f6",
        confirmButtonText: "Evet, Sil",
        cancelButtonText: "İptal",
      });
      if (result.isConfirmed) remove(user.id);
    },
    [remove],
  );

  const promptReset = useCallback(
    async (user: ManagedUser) => {
      const result = await Swal.fire({
        title: `${user.username} için yeni geçici şifre`,
        input: "password",
        inputPlaceholder: "En az 8 karakter",
        inputValidator: (value) =>
          !value || value.length < 8 ? "Şifre en az 8 karakter olmalı" : undefined,
        showCancelButton: true,
        confirmButtonText: "Sıfırla",
        cancelButtonText: "İptal",
      });
      if (result.isConfirmed && typeof result.value === "string") {
        reset({ id: user.id, password: result.value });
      }
    },
    [reset],
  );

  const columns = useMemo<DataTableColumnDef<ManagedUser>[]>(
    () => [
      {
        accessorKey: "username",
        header: "Kullanıcı",
        cell: ({ row }) => (
          <div className="min-w-[8rem]">
            <div className="font-medium text-text-primary">
              {row.original.username}
              {row.original.username === currentUsername && (
                <span className="ml-2 text-xs text-text-muted">(siz)</span>
              )}
            </div>
            {row.original.email && (
              <div className="text-xs text-text-muted">{row.original.email}</div>
            )}
          </div>
        ),
      },
      {
        accessorKey: "role",
        header: "Rol",
        cell: ({ row }) => {
          const user = row.original;
          if (user.username === currentUsername) {
            return <Badge variant={roleVariant(user.role)}>{ROLE_LABELS[user.role]}</Badge>;
          }
          return (
            <select
              aria-label={`${user.username} rolü`}
              className={selectClass}
              value={user.role}
              disabled={isMutating}
              onChange={(e) => setRole({ id: user.id, role: e.target.value as Role })}
            >
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {ROLE_LABELS[role]}
                </option>
              ))}
            </select>
          );
        },
      },
      {
        accessorKey: "mustChangePassword",
        header: "Durum",
        cell: ({ row }) =>
          row.original.mustChangePassword ? (
            <Badge variant="warning">Şifre değişikliği bekliyor</Badge>
          ) : (
            <Badge variant="success">Aktif</Badge>
          ),
      },
      {
        id: "actions",
        header: "Aksiyon",
        cell: ({ row }) => {
          const user = row.original;
          if (user.username === currentUsername) return null;
          return (
            <div className="flex items-center gap-1 whitespace-nowrap">
              <Button
                variant="ghost"
                size="sm"
                icon={<KeyRound size={14} />}
                onClick={() => void promptReset(user)}
                disabled={isMutating}
                title="Şifreyi Sıfırla"
                aria-label="Şifreyi Sıfırla"
              />
              <Button
                variant="ghost"
                size="sm"
                className="text-error hover:bg-error/10 hover:text-error"
                icon={<Trash2 size={14} />}
                onClick={() => void confirmDelete(user)}
                disabled={isMutating}
                title="Sil"
                aria-label="Sil"
              />
            </div>
          );
        },
      },
    ],
    [currentUsername, isMutating, setRole, promptReset, confirmDelete],
  );

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-semibold text-text-primary">Kullanıcılar</h2>
        <p className="text-sm text-text-muted mt-1">
          Rol değişiklikleri kullanıcının oturumu yenilendiğinde geçerli olur.
        </p>
      </div>

      <DataTable
        data={users ?? []}
        columns={columns}
        loading={isLoading}
        emptyMessage="Kullanıcı yok."
      />

      <form
        onSubmit={handleSubmit(onCreate)}
        className="space-y-4 border-t border-border-default pt-6"
        noValidate
      >
        <h3 className="font-medium text-text-primary">Yeni Kullanıcı</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input label="Kullanıcı Adı" error={errors.username?.message} {...register("username")} />
          <Input
            label="E-posta (isteğe bağlı)"
            id="new-user-email"
            type="email"
            error={errors.email?.message}
            {...register("email")}
          />
          <Input
            label="Geçici Şifre"
            id="new-user-password"
            type="password"
            autoComplete="new-password"
            hint="Kullanıcı ilk girişte değiştirmek zorunda."
            error={errors.password?.message}
            {...register("password")}
          />
          <div className="flex flex-col gap-1.5">
            <label htmlFor="new-user-role" className="text-sm font-medium text-text-secondary">
              Rol
            </label>
            <select id="new-user-role" className={selectClass} {...register("role")}>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {ROLE_LABELS[role]} — {ROLE_DESCRIPTIONS[role]}
                </option>
              ))}
            </select>
          </div>
        </div>
        <Button type="submit" icon={<UserPlus size={16} />} loading={createUser.isPending}>
          Kullanıcı Ekle
        </Button>
      </form>
    </div>
  );
}
