import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";

export interface GetAnalysesParams {
  sourceId?: number;
  page?: number;
  size?: number;
}

export const AnalysisApi = {
  getAnalyses: async (params?: GetAnalysesParams): Promise<{ content: AnalysisResultDto[], totalPages: number, totalElements?: number }> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.ANALYSES.LIST,
      params,
    });
    return {
      content: response.data.data.content || response.data.data || [],
      totalPages: response.data.data.totalPages || response.data.data.total_pages || 1,
      totalElements: response.data.data.totalElements || response.data.data.total_elements || 0,
    };
  },

  triggerAnalysis: async (groupId: number): Promise<any> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.ANALYSES.TRIGGER(groupId),
    });
    return response.data.data;
  },

  deleteHistory: async (sourceId?: number): Promise<void> => {
    await apiClient({
      method: HttpMethod.DELETE,
      url: Endpoints.ANALYSES.DELETE_HISTORY,
      params: sourceId ? { sourceId } : undefined,
    });
  },
};
