import { useMutation, useQueryClient } from "@tanstack/react-query";
import { SourceApi } from "./sourceApi";
import { sourceKeys } from "./useSourceQuery";
import type { LogSourceDto } from "@/features/sources/types/source.types";

export const useTestConnectionMutation = () => {
  return useMutation({
    mutationFn: async (id: number) => {
      try {
        return await SourceApi.testConnection(id);
      } catch (error: any) {
        throw new Error(error.response?.data?.error?.message || error.message || "Bağlantı kurulamadı");
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
      } catch (error: any) {
        throw new Error(error.response?.data?.error?.message || error.message || "Bağlantı kurulamadı");
      }
    }
  });
};
