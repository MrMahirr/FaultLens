import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockDeployments, type MockDeployment } from "@/shared/mocks/data";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";

export const deploymentKeys = {
  all: ["deployments"] as const,
  list: () => [...deploymentKeys.all, "list"] as const,
};

const fetchDeployments = async (): Promise<MockDeployment[]> => {
  try {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.DEPLOYMENTS.LIST,
    });
    return response.data.data.content || response.data.data;
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      console.warn("Backend connection failed, falling back to mock deployments.");
      return mockDeployments;
    }
    throw error;
  }
};

export const useDeployments = () =>
  useQuery({ queryKey: deploymentKeys.list(), queryFn: fetchDeployments });

export const useCreateDeployment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: Partial<MockDeployment>) => {
      try {
        const response = await apiClient({
          method: HttpMethod.POST,
          url: Endpoints.DEPLOYMENTS.CREATE,
          data,
        });
        return response.data.data;
      } catch (error: any) {
        if (
          process.env.NODE_ENV === "development" &&
          (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
        ) {
          await new Promise((resolve) => setTimeout(resolve, 800));
          return { ...data, id: Date.now() } as MockDeployment;
        }
        throw error;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: deploymentKeys.list() });
    },
  });
};
