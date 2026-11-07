import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  AuthApi,
  type ChangePasswordRequest,
  type LoginRequest,
  type LoginResponse,
} from "./authApi";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { decodeJwt } from "@/shared/lib/jwt";
import { getApiErrorMessage } from "@/shared/api/errors";

export const useLoginMutation = () => {
  const loginStore = useAuthStore.getState().login;
  const t = useTranslations("auth.login");

  return useMutation({
    mutationFn: async (data: LoginRequest) => {
      try {
        return await AuthApi.login(data);
      } catch (error) {
        // Surface the backend message (e.g. "Invalid username or password") instead of
        // axios' generic "Request failed with status code 401".
        throw new Error(getApiErrorMessage(error, t("invalid")));
      }
    },
    onSuccess: (data: LoginResponse) => {
      const payload = decodeJwt(data.token);
      let username = "admin";
      let role = "VIEWER";

      if (payload) {
        username = payload.sub;
        role = payload.roles?.[0]?.replace("ROLE_", "") || "VIEWER";
      }

      loginStore(data.token, {
        username,
        role,
        mustChangePassword: data.mustChangePassword,
      });
    },
  });
};

/**
 * Changes the current user's password and swaps in the fresh token returned by
 * the backend (the old one may still carry the password-change restriction).
 */
export const useChangePasswordMutation = () => {
  return useMutation({
    mutationFn: (data: ChangePasswordRequest) => AuthApi.changePassword(data),
    onSuccess: (data: LoginResponse) => {
      const { user, login } = useAuthStore.getState();
      if (user) {
        login(data.token, { ...user, mustChangePassword: data.mustChangePassword });
      }
    },
  });
};
