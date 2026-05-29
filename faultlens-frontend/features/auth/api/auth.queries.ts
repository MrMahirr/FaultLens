import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { decodeJwt } from "@/shared/lib/jwt";

/* ── Types ─────────────────────────────────────────────────── */

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  expiresAt: string;
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
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
    };
  }

  throw new Error("Geçersiz kullanıcı adı veya şifre");
};

/* ── Mutations ─────────────────────────────────────────────── */

export const useLogin = () => {
  const login = useAuthStore.getState().login;

  return useMutation({
    mutationFn: async (data: LoginRequest): Promise<LoginResponse> => {
      try {
        const response = await apiClient({
          method: HttpMethod.POST,
          url: Endpoints.AUTH.LOGIN,
          data,
        });
        return response.data.data;
      } catch (error: any) {
        // If connection is refused, network error, or backend is offline, fallback to mock in dev mode
        if (
          process.env.NODE_ENV === "development" &&
          (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
        ) {
          console.warn("Backend connection failed, falling back to mock authentication.");
          return mockLogin(data);
        }
        const message = error.response?.data?.error?.message || error.message || "Geçersiz kullanıcı adı veya şifre";
        throw new Error(message);
      }
    },
    onSuccess: (data) => {
      // Decode JWT to extract username and role (stateless auth)
      const payload = decodeJwt(data.token);
      
      let username = "admin";
      let role = "ADMIN";
      
      if (payload) {
        username = payload.sub;
        role = payload.roles?.[0]?.replace('ROLE_', '') || "ADMIN";
      }

      login(data.token, { username, role });
    },
  });
};

export type { LoginRequest, LoginResponse };
