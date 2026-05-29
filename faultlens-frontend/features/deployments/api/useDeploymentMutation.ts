import { useMutation, useQueryClient } from "@tanstack/react-query";
import { DeploymentApi } from "./deploymentApi";
import { deploymentKeys } from "./useDeploymentQuery";
import type { DeploymentDto } from "@/features/deployments/types/deployment.types";

export const useCreateDeploymentMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<DeploymentDto>) => DeploymentApi.createDeployment(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: deploymentKeys.list() });
    },
  });
};
