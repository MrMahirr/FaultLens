import { useQuery } from "@tanstack/react-query";
import { AnalysisApi } from "./analysisApi";

export const analysisKeys = {
  all: ["analyses"] as const,
  list: () => [...analysisKeys.all, "list"] as const,
  detail: (id: number) => [...analysisKeys.all, "detail", id] as const,
};

export const useAnalysesQuery = () =>
  useQuery({
    queryKey: analysisKeys.list(),
    queryFn: () => AnalysisApi.getAnalyses(),
  });
