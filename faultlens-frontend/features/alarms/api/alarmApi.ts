import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { PagedResponse } from "@/shared/types/api.types";
import type { Alarm, AlarmFilters, AlarmStats } from "../types/alarm.types";

/**
 * Alarm REST client. Responses use the standard ApiResponse wrapper
 * (`{ success, data }`); the axios client already converts snake_case.
 */
export const AlarmApi = {
  list: async (filters: AlarmFilters): Promise<PagedResponse<Alarm>> => {
    const params: Record<string, string | number> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };
    if (filters.status) params.status = filters.status;
    if (filters.severity) params.severity = filters.severity;

    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.ALARMS.LIST,
      params,
    });
    return response.data.data;
  },

  stats: async (): Promise<AlarmStats> => {
    const response = await apiClient({ method: HttpMethod.GET, url: Endpoints.ALARMS.STATS });
    return response.data.data;
  },

  acknowledge: async (id: number): Promise<Alarm> => {
    const response = await apiClient({ method: HttpMethod.PUT, url: Endpoints.ALARMS.ACK(id) });
    return response.data.data;
  },

  resolve: async (id: number): Promise<Alarm> => {
    const response = await apiClient({ method: HttpMethod.PUT, url: Endpoints.ALARMS.RESOLVE(id) });
    return response.data.data;
  },

  remove: async (id: number): Promise<void> => {
    await apiClient({ method: HttpMethod.DELETE, url: Endpoints.ALARMS.DELETE(id) });
  },
};
