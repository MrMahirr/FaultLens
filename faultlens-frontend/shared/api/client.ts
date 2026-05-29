import axios from "axios";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { snakeToCamel, camelToSnake } from "@/shared/lib/caseTransform";

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
// 1. Bearer token ekleme
// 2. Request body camelCase → snake_case dönüşümü
apiClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // camelCase → snake_case (backend @JsonNaming SnakeCaseStrategy bekliyor)
    if (config.data && typeof config.data === "object") {
      config.data = camelToSnake(config.data);
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response Interceptor ────────────────────────────────────
// 1. Response data snake_case → camelCase dönüşümü
// 2. 401 durumunda logout ve redirect
apiClient.interceptors.response.use(
  (response) => {
    // snake_case → camelCase (backend @JsonNaming SnakeCaseStrategy döndürüyor)
    if (response.data) {
      response.data = snakeToCamel(response.data);
    }
    return response;
  },
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
