import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockAnalyses, type MockAnalysis } from "@/shared/mocks/data";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";

export const analysisKeys = {
  all: ["analyses"] as const,
  list: () => [...analysisKeys.all, "list"] as const,
  detail: (id: number) => [...analysisKeys.all, "detail", id] as const,
};

const fetchAnalyses = async (): Promise<MockAnalysis[]> => {
  try {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.ANALYSES.LIST,
    });
    return response.data.data.content || response.data.data;
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      console.warn("Backend connection failed, falling back to mock analyses.");
      return mockAnalyses;
    }
    throw error;
  }
};

export const useAnalyses = () =>
  useQuery({ queryKey: analysisKeys.list(), queryFn: fetchAnalyses });

export const useTriggerAnalysis = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (groupId: number) => {
      try {
        const response = await apiClient({
          method: HttpMethod.POST,
          url: Endpoints.ANALYSES.TRIGGER(groupId),
        });
        return response.data.data;
      } catch (error: any) {
        if (
          process.env.NODE_ENV === "development" &&
          (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
        ) {
          await new Promise((resolve) => setTimeout(resolve, 1000));
          return { success: true };
        }
        throw error;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: analysisKeys.all });
    },
  });
};
