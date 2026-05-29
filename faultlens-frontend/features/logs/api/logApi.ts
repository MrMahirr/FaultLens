import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { LogEntryDto, LogFilters, LogGroupDto, LogDetailDto } from "@/features/logs/types/log.types";
import type { PagedResponse } from "@/shared/types/api.types";

export const LogApi = {
  getLogs: async (filters: LogFilters): Promise<PagedResponse<LogEntryDto>> => {
    const params: Record<string, any> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };

    if (filters.severity && filters.severity.length > 0) {
      params.severity = filters.severity[0];
    }
    if (filters.search) {
      params.search = filters.search;
    }
    if (filters.source) {
      const sourceIdNum = Number(filters.source);
      if (!isNaN(sourceIdNum)) {
        params.sourceId = sourceIdNum;
      }
    }
    if (filters.startDate) {
      params.from = filters.startDate;
    }
    if (filters.endDate) {
      params.to = filters.endDate;
    }

    const url = filters.groupId
      ? Endpoints.LOGS.GROUP_ENTRIES(filters.groupId)
      : Endpoints.LOGS.LIST;

    const response = await apiClient({
      method: HttpMethod.GET,
      url,
      params,
    });

    return response.data.data;
  },

  getLogDetail: async (id: number): Promise<LogDetailDto> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.DETAIL(id),
    });
    return response.data.data;
  },

  getLogGroups: async (): Promise<PagedResponse<LogGroupDto>> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.GROUPS,
    });
    return response.data.data;
  },

  clearLogs: async (sourceId: number): Promise<void> => {
    await apiClient({
      method: HttpMethod.DELETE,
      url: `${Endpoints.LOGS.LIST}/clear`,
      params: { sourceId },
    });
  },
};
