import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";

export interface SystemSettingsDto {
  logRetentionDays: number;
  defaultEngine: string;
  language: string;
  aiAvailable: boolean;
}

export const systemSettingsKey = ["settings", "system"] as const;

/** Instance-wide settings (any signed-in user may read them; only ADMIN may change them). */
export const useSystemSettingsQuery = (enabled = true) =>
  useQuery({
    queryKey: systemSettingsKey,
    queryFn: async () =>
      (await apiClient.get<{ data: SystemSettingsDto }>(Endpoints.SETTINGS.SYSTEM)).data.data,
    staleTime: 5 * 60_000,
    enabled,
  });
