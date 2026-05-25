import { QueryClient } from "@tanstack/react-query";

/**
 * TanStack Query client singleton.
 * - staleTime: 30 saniye (30s içinde aynı query tekrar fetch edilmez)
 * - gcTime: 5 dakika (cache'te 5 dk tutulur)
 * - retry: 2 (başarısız query 2 kez daha denenir)
 * - refetchOnWindowFocus: kapalı (tab değişiminde otomatik refetch yok)
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,
      gcTime: 1000 * 60 * 5,
      retry: 2,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 1,
    },
  },
});
