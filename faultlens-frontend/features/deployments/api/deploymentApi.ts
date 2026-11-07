import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { DeploymentDto } from "@/features/deployments/types/deployment.types";
import type { ApiResponse, PagedResponse } from "@/shared/types/api.types";

export const DeploymentApi = {
  /** Latest page (API default: 50, newest first); GET /deployments is paged. */
  getDeployments: async (): Promise<DeploymentDto[]> => {
    const response = await apiClient<ApiResponse<PagedResponse<DeploymentDto>>>({
      method: HttpMethod.GET,
      url: Endpoints.DEPLOYMENTS.LIST,
    });
    return response.data.data.content;
  },

  createDeployment: async (data: Partial<DeploymentDto>): Promise<DeploymentDto> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.DEPLOYMENTS.CREATE,
      data,
    });
    return response.data.data;
  },
};
