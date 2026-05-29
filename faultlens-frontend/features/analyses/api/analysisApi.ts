import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";

export const AnalysisApi = {
  getAnalyses: async (): Promise<AnalysisResultDto[]> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.ANALYSES.LIST,
    });
    return response.data.data.content || response.data.data;
  },

  triggerAnalysis: async (groupId: number): Promise<any> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.ANALYSES.TRIGGER(groupId),
    });
    return response.data.data;
  },
};
