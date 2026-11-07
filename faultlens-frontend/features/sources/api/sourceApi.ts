import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { LogSourceDto } from "@/features/sources/types/source.types";

export const SourceApi = {
  getSources: async (): Promise<LogSourceDto[]> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.SOURCES.LIST,
    });
    return response.data.data;
  },

  testConnection: async (id: number): Promise<{ success: boolean; message: string }> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.SOURCES.TEST(id),
    });
    const isConnected = response.data.data?.connected;
    if (isConnected === false) throw new Error("connection test failed"); // UI shows its own translated text
    return { success: true, message: "connected" };
  },

  testConfig: async (
    data: Partial<LogSourceDto>,
  ): Promise<{ success: boolean; message: string }> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.SOURCES.TEST_CONFIG,
      data,
    });
    const isConnected = response.data.data?.connected;
    if (isConnected === false) throw new Error("connection test failed"); // UI shows its own translated text
    return { success: true, message: "connected" };
  },

  createSource: async (data: Partial<LogSourceDto>): Promise<LogSourceDto> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.SOURCES.CREATE,
      data,
    });
    return response.data.data;
  },

  setEnabled: async (id: number, enabled: boolean): Promise<LogSourceDto> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: enabled ? Endpoints.SOURCES.ENABLE(id) : Endpoints.SOURCES.DISABLE(id),
    });
    return response.data.data;
  },

  deleteSource: async (id: number): Promise<void> => {
    await apiClient({ method: HttpMethod.DELETE, url: Endpoints.SOURCES.DETAIL(id) });
  },
};
