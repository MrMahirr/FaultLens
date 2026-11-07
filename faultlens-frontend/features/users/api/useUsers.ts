import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { Role } from "@/features/auth/hooks/useCan";
import type { CreateUserInput } from "../types/user.types";
import { UserApi } from "./userApi";

export const userKeys = { all: ["users"] as const };

export const useUsersQuery = () => useQuery({ queryKey: userKeys.all, queryFn: UserApi.list });

type ToastKey =
  | "created"
  | "createFailed"
  | "roleChanged"
  | "roleFailed"
  | "reset"
  | "resetFailed"
  | "deleted"
  | "deleteFailed";

function useUserMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<unknown>,
  successKey: ToastKey,
  errorKey: ToastKey,
) {
  const queryClient = useQueryClient();
  const t = useTranslations("settings.users.toast");
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.all });
      toast.success(t(successKey));
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, t(errorKey)));
    },
  });
}

export const useCreateUserMutation = () =>
  useUserMutation((input: CreateUserInput) => UserApi.create(input), "created", "createFailed");

export const useChangeRoleMutation = () =>
  useUserMutation(
    ({ id, role }: { id: number; role: Role }) => UserApi.changeRole(id, role),
    "roleChanged",
    "roleFailed",
  );

export const useResetPasswordMutation = () =>
  useUserMutation(
    ({ id, password }: { id: number; password: string }) => UserApi.resetPassword(id, password),
    "reset",
    "resetFailed",
  );

export const useDeleteUserMutation = () =>
  useUserMutation((id: number) => UserApi.remove(id), "deleted", "deleteFailed");
