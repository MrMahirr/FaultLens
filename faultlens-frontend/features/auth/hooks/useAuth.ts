"use client";

import { useAuthStore } from "@/features/auth/store/auth.store";

/**
 * Auth accessor hook.
 * Component'larda auth durumuna erişim için kullanılır.
 */
export function useAuth() {
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const login = useAuthStore((state) => state.login);
  const logout = useAuthStore((state) => state.logout);

  return {
    token,
    user,
    login,
    logout,
    isAuthenticated: token !== null,
  };
}
