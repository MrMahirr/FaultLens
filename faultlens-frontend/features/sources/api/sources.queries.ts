import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockSources, type MockSource } from "@/shared/mocks/data";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";

export const sourceKeys = {
  all: ["sources"] as const,
  list: () => [...sourceKeys.all, "list"] as const,
};

const fetchSources = async (): Promise<MockSource[]> => {
  try {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.SOURCES.LIST,
    });
    return response.data.data;
  } catch (error: any) {
    if (
      process.env.NODE_ENV === "development" &&
      (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
    ) {
      console.warn("Backend connection failed, falling back to mock sources.");
      return mockSources;
    }
    throw error;
  }
};

export const useSources = () =>
  useQuery({ queryKey: sourceKeys.list(), queryFn: fetchSources });

export const useTestConnection = () => {
  return useMutation({
    mutationFn: async (id: number) => {
      try {
        const response = await apiClient({
          method: HttpMethod.POST,
          url: Endpoints.SOURCES.TEST(id),
        });
        const isConnected = response.data.data.connected;
        if (!isConnected) throw new Error("Bağlantı testi başarısız");
        return { success: true, message: "Bağlantı başarılı" };
      } catch (error: any) {
        if (
          process.env.NODE_ENV === "development" &&
          (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
        ) {
          await new Promise((resolve) => setTimeout(resolve, 1200));
          return { success: true, message: "Bağlantı başarılı (Mock)" };
        }
        throw new Error(error.response?.data?.message || error.message || "Bağlantı kurulamadı");
      }
    },
  });
};

export const useCreateSource = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: Partial<MockSource>) => {
      try {
        const response = await apiClient({
          method: HttpMethod.POST,
          url: Endpoints.SOURCES.CREATE,
          data,
        });
        return response.data.data;
      } catch (error: any) {
        if (
          process.env.NODE_ENV === "development" &&
          (!error.response || error.code === "ERR_NETWORK" || error.message?.includes("Network Error"))
        ) {
          await new Promise((resolve) => setTimeout(resolve, 800));
          return { ...data, id: Date.now() } as MockSource;
        }
        throw error;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
    },
  });
};
