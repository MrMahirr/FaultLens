import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { Role } from "@/features/auth/hooks/useCan";
import type { CreateUserInput } from "../types/user.types";
import { UserApi } from "./userApi";

export const userKeys = { all: ["users"] as const };

export const useUsersQuery = () => useQuery({ queryKey: userKeys.all, queryFn: UserApi.list });

function useUserMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<unknown>,
  successMessage: string,
  errorMessage: string,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.all });
      toast.success(successMessage);
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, errorMessage));
    },
  });
}

export const useCreateUserMutation = () =>
  useUserMutation(
    (input: CreateUserInput) => UserApi.create(input),
    "Kullanıcı oluşturuldu; ilk girişte şifresini değiştirecek.",
    "Kullanıcı oluşturulamadı.",
  );

export const useChangeRoleMutation = () =>
  useUserMutation(
    ({ id, role }: { id: number; role: Role }) => UserApi.changeRole(id, role),
    "Rol güncellendi. Değişiklik kullanıcının bir sonraki oturum yenilemesinde geçerli olur.",
    "Rol güncellenemedi.",
  );

export const useResetPasswordMutation = () =>
  useUserMutation(
    ({ id, password }: { id: number; password: string }) => UserApi.resetPassword(id, password),
    "Şifre sıfırlandı; kullanıcı ilk girişte değiştirecek.",
    "Şifre sıfırlanamadı.",
  );

export const useDeleteUserMutation = () =>
  useUserMutation(
    (id: number) => UserApi.remove(id),
    "Kullanıcı silindi.",
    "Kullanıcı silinemedi.",
  );
