import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { ApiKey, CreateApiKeyInput, CreatedApiKey } from "../types/apiKey.types";

export const apiKeyKeys = { all: ["api-keys"] as const };

/** API key management REST client (/api/v1/api-keys, ADMIN only). */
export const ApiKeyApi = {
  list: async (): Promise<ApiKey[]> =>
    (await apiClient({ method: HttpMethod.GET, url: Endpoints.API_KEYS.LIST })).data.data,

  create: async (input: CreateApiKeyInput): Promise<CreatedApiKey> =>
    (await apiClient({ method: HttpMethod.POST, url: Endpoints.API_KEYS.LIST, data: input })).data
      .data,

  revoke: async (id: number): Promise<void> => {
    await apiClient({ method: HttpMethod.DELETE, url: Endpoints.API_KEYS.DETAIL(id) });
  },
};

export const useApiKeysQuery = () =>
  useQuery({ queryKey: apiKeyKeys.all, queryFn: ApiKeyApi.list });

export const useCreateApiKeyMutation = () => {
  const queryClient = useQueryClient();
  const t = useTranslations("settings.apiKeys.toast");
  return useMutation({
    mutationFn: ApiKeyApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: apiKeyKeys.all }),
    onError: (error) => toast.error(getApiErrorMessage(error, t("createFailed"))),
  });
};

export const useRevokeApiKeyMutation = () => {
  const queryClient = useQueryClient();
  const t = useTranslations("settings.apiKeys.toast");
  return useMutation({
    mutationFn: ApiKeyApi.revoke,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.all });
      toast.success(t("revoked"));
    },
    onError: (error) => toast.error(getApiErrorMessage(error, t("revokeFailed"))),
  });
};
