import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
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
    },
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
    },
  });
};

export const useToggleSourceMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      SourceApi.setEnabled(id, enabled),
    onSuccess: (_data, { enabled }) => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
      toast.success(enabled ? "Kaynak etkinleştirildi." : "Kaynak devre dışı bırakıldı.");
    },
    onError: (error) => toast.error(getApiErrorMessage(error, "Kaynak güncellenemedi.")),
  });
};

export const useDeleteSourceMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => SourceApi.deleteSource(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
      toast.success("Kaynak silindi.");
    },
    onError: (error) => toast.error(getApiErrorMessage(error, "Kaynak silinemedi.")),
  });
};
