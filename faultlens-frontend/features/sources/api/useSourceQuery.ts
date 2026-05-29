import { useQuery } from "@tanstack/react-query";
import { SourceApi } from "./sourceApi";

export const sourceKeys = {
  all: ["sources"] as const,
  list: () => [...sourceKeys.all, "list"] as const,
};

export const useSourcesQuery = () =>
  useQuery({
    queryKey: sourceKeys.list(),
    queryFn: () => SourceApi.getSources(),
  });
