import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { DeploymentDto } from "@/features/deployments/types/deployment.types";

export const DeploymentApi = {
  getDeployments: async (): Promise<DeploymentDto[]> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.DEPLOYMENTS.LIST,
    });
    return response.data.data.content || response.data.data;
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
