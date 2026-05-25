import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockSources, type MockSource } from "@/shared/mocks/data";

export const sourceKeys = {
  all: ["sources"] as const,
  list: () => [...sourceKeys.all, "list"] as const,
};

const fetchSources = async (): Promise<MockSource[]> => {
  await new Promise((resolve) => setTimeout(resolve, 400));
  return mockSources;
};

export const useSources = () =>
  useQuery({ queryKey: sourceKeys.list(), queryFn: fetchSources });

export const useTestConnection = () => {
  return useMutation({
    mutationFn: async (id: number) => {
      await new Promise((resolve) => setTimeout(resolve, 1500));
      return { success: true, message: "Bağlantı başarılı" };
    },
  });
};

export const useCreateSource = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: Partial<MockSource>) => {
      await new Promise((resolve) => setTimeout(resolve, 800));
      return { ...data, id: Date.now() } as MockSource;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourceKeys.list() });
    },
  });
};
