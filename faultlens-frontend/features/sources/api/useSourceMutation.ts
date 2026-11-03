import { useMutation, useQueryClient } from "@tanstack/react-query";
import { SourceApi } from "./sourceApi";
import { sourceKeys } from "./useSourceQuery";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import { getApiErrorMessage } from "@/shared/api/errors";

export const useTestConnectionMutation = () => {
  return useMutation({
    mutationFn: async (id: number) => {
      try {
        return await SourceApi.testConnection(id);
      } catch (error) {
        throw new Error(getApiErrorMessage(error, "Bağlantı kurulamadı"));
      }
    }
  });
};

export const useCreateSourceMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<LogSourceDto>) => SourceApi.createSource(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
    },
  });
};

export const useTestConfigMutation = () => {
  return useMutation({
    mutationFn: async (data: Partial<LogSourceDto>) => {
      try {
        return await SourceApi.testConfig(data);
      } catch (error) {
        throw new Error(getApiErrorMessage(error, "Bağlantı kurulamadı"));
      }
    }
  });
};
