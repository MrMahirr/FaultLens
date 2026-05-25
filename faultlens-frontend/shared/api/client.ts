import axios from "axios";

/**
 * Axios API client instance.
 * - Request interceptor: her isteğe Bearer token ekler
 * - Response interceptor: 401'de logout ve /login'e redirect
 *
 * Auth store import'u lazy yapılır (circular dependency önlemi).
 */
export const apiClient = axios.create({
  headers: { "Content-Type": "application/json" },
  timeout: 10_000,
});

// ── Request Interceptor ─────────────────────────────────────
apiClient.interceptors.request.use(
  (config) => {
    // Lazy import to avoid circular dependency with auth store
    const authStoreModule = require("@/features/auth/store/auth.store");
    const token = authStoreModule.useAuthStore.getState().token;

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
      const authStoreModule = require("@/features/auth/store/auth.store");
      authStoreModule.useAuthStore.getState().logout();

      // Client-side redirect
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);
