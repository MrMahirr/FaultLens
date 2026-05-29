import { useMutation } from "@tanstack/react-query";
import { AuthApi, type LoginRequest, type LoginResponse } from "./authApi";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { decodeJwt } from "@/shared/lib/jwt";

export const useLoginMutation = () => {
  const loginStore = useAuthStore.getState().login;

  return useMutation({
    mutationFn: (data: LoginRequest) => AuthApi.login(data),
    onSuccess: (data: LoginResponse) => {
      const payload = decodeJwt(data.token);
      let username = "admin";
      let role = "ADMIN";
      
      if (payload) {
        username = payload.sub;
        role = payload.roles?.[0]?.replace('ROLE_', '') || "ADMIN";
      }

      loginStore(data.token, { username, role });
    },
    onError: (error: any) => {
      const message = error.response?.data?.error?.message || error.message || "Geçersiz kullanıcı adı veya şifre";
      throw new Error(message);
    }
  });
};
