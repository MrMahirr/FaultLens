import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockAnalyses, type MockAnalysis } from "@/shared/mocks/data";

export const analysisKeys = {
  all: ["analyses"] as const,
  list: () => [...analysisKeys.all, "list"] as const,
  detail: (id: number) => [...analysisKeys.all, "detail", id] as const,
};

const fetchAnalyses = async (): Promise<MockAnalysis[]> => {
  await new Promise((resolve) => setTimeout(resolve, 500));
  return mockAnalyses;
};

export const useAnalyses = () =>
  useQuery({ queryKey: analysisKeys.list(), queryFn: fetchAnalyses });

export const useTriggerAnalysis = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (groupId: number) => {
      await new Promise((resolve) => setTimeout(resolve, 2000));
      return { success: true };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: analysisKeys.all });
    },
  });
};
