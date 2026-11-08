import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { DeploymentDto } from "@/features/deployments/types/deployment.types";
import type { ApiResponse, PagedResponse } from "@/shared/types/api.types";

export interface DeploymentListParams {
  /** Part of the service name, case-insensitive. */
  q?: string;
  environment?: string;
  /** Zero-based. */
  page: number;
  size: number;
}

export const DeploymentApi = {
  /** One page, newest first; the service search and environment filter run on the server. */
  getDeployments: async (params: DeploymentListParams): Promise<PagedResponse<DeploymentDto>> => {
    const response = await apiClient<ApiResponse<PagedResponse<DeploymentDto>>>({
      method: HttpMethod.GET,
      url: Endpoints.DEPLOYMENTS.LIST,
      params: {
        q: params.q || undefined,
        environment: params.environment || undefined,
        page: params.page,
        size: params.size,
      },
    });
    return response.data.data;
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
