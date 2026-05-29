import { useQuery } from "@tanstack/react-query";
import { DeploymentApi } from "./deploymentApi";

export const deploymentKeys = {
  all: ["deployments"] as const,
  list: () => [...deploymentKeys.all, "list"] as const,
};

export const useDeploymentsQuery = () =>
  useQuery({
    queryKey: deploymentKeys.list(),
    queryFn: () => DeploymentApi.getDeployments(),
  });
