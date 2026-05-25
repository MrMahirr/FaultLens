import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import { useAuthStore } from "@/features/auth/store/auth.store";

/* ── Types ─────────────────────────────────────────────────── */

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  user: {
    username: string;
    role: string;
  };
}

/* ── Mock Login ────────────────────────────────────────────── */

const MOCK_CREDENTIALS = {
  username: "admin",
  password: "admin123",
};

const mockLogin = async (data: LoginRequest): Promise<LoginResponse> => {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 800));

  if (
    data.username === MOCK_CREDENTIALS.username &&
    data.password === MOCK_CREDENTIALS.password
  ) {
    return {
      token: "mock-jwt-token-" + Date.now(),
      user: {
        username: data.username,
        role: "ADMIN",
      },
    };
  }

  throw new Error("Geçersiz kullanıcı adı veya şifre");
};

/* ── Mutations ─────────────────────────────────────────────── */

export const useLogin = () => {
  const login = useAuthStore.getState().login;

  return useMutation({
    mutationFn: async (data: LoginRequest): Promise<LoginResponse> => {
      // Use mock for development
      if (process.env.NODE_ENV === "development") {
        return mockLogin(data);
      }

      const response = await apiClient({
        method: HttpMethod.POST,
        url: Endpoints.AUTH.LOGIN,
        data,
      });
      return response.data.data;
    },
    onSuccess: (data) => {
      login(data.token, data.user);
    },
  });
};

export type { LoginRequest, LoginResponse };
