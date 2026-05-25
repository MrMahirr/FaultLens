import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockDeployments, type MockDeployment } from "@/shared/mocks/data";

export const deploymentKeys = {
  all: ["deployments"] as const,
  list: () => [...deploymentKeys.all, "list"] as const,
};

const fetchDeployments = async (): Promise<MockDeployment[]> => {
  await new Promise((resolve) => setTimeout(resolve, 400));
  return mockDeployments;
};

export const useDeployments = () =>
  useQuery({ queryKey: deploymentKeys.list(), queryFn: fetchDeployments });

export const useCreateDeployment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: Partial<MockDeployment>) => {
      await new Promise((resolve) => setTimeout(resolve, 800));
      return { ...data, id: Date.now() } as MockDeployment;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: deploymentKeys.list() });
    },
  });
};
