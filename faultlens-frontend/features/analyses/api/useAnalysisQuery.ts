import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { AnalysisApi, GetAnalysesParams } from "./analysisApi";

export const analysisKeys = {
  all: ["analyses"] as const,
  list: (params?: GetAnalysesParams) => [...analysisKeys.all, "list", params] as const,
  detail: (id: number) => [...analysisKeys.all, "detail", id] as const,
};

export const useAnalysesQuery = (params?: GetAnalysesParams) =>
  useQuery({
    queryKey: analysisKeys.list(params),
    queryFn: () => AnalysisApi.getAnalyses(params),
  });

export const useDeleteAnalysisHistory = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sourceId?: number) => AnalysisApi.deleteHistory(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: analysisKeys.all });
    },
  });
};
