import axios from "axios";
import { useAuthStore } from "@/features/auth/store/auth.store";

/**
 * Axios API client instance.
 * - Request interceptor: her isteğe Bearer token ekler
 * - Response interceptor: 401'de logout ve /login'e redirect
 */
export const apiClient = axios.create({
  headers: { "Content-Type": "application/json" },
  timeout: 10_000,
});

// ── Request Interceptor ─────────────────────────────────────
apiClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response Interceptor ────────────────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();

      // Client-side redirect
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);
