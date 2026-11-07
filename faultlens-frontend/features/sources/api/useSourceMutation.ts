import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { useTranslations } from "next-intl";
import { SourceApi } from "./sourceApi";
import { sourceKeys } from "./useSourceQuery";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import { getApiErrorMessage } from "@/shared/api/errors";

export const useTestConnectionMutation = () => {
  const t = useTranslations("sources.toast");
  return useMutation({
    mutationFn: async (id: number) => {
      try {
        return await SourceApi.testConnection(id);
      } catch (error) {
        throw new Error(getApiErrorMessage(error, t("connectFailed")));
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
  const t = useTranslations("sources.toast");
  return useMutation({
    mutationFn: async (data: Partial<LogSourceDto>) => {
      try {
        return await SourceApi.testConfig(data);
      } catch (error) {
        throw new Error(getApiErrorMessage(error, t("connectFailed")));
      }
    },
  });
};

export const useToggleSourceMutation = () => {
  const queryClient = useQueryClient();
  const t = useTranslations("sources.toast");
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      SourceApi.setEnabled(id, enabled),
    onSuccess: (_data, { enabled }) => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
      toast.success(enabled ? t("enabled") : t("disabled"));
    },
    onError: (error) => toast.error(getApiErrorMessage(error, t("updateFailed"))),
  });
};

export const useDeleteSourceMutation = () => {
  const queryClient = useQueryClient();
  const t = useTranslations("sources.toast");
  return useMutation({
    mutationFn: (id: number) => SourceApi.deleteSource(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
      toast.success(t("deleted"));
    },
    onError: (error) => toast.error(getApiErrorMessage(error, t("deleteFailed"))),
  });
};
