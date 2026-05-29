import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AnalysisApi } from "./analysisApi";
import { analysisKeys } from "./useAnalysisQuery";

export const useTriggerAnalysisMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) => AnalysisApi.triggerAnalysis(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: analysisKeys.all });
    },
  });
};
