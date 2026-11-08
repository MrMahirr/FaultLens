import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { DeploymentApi, type DeploymentListParams } from "./deploymentApi";

export const deploymentKeys = {
  all: ["deployments"] as const,
  list: () => [...deploymentKeys.all, "list"] as const,
  page: (params: DeploymentListParams) => [...deploymentKeys.list(), params] as const,
};

/** Keeps the previous page on screen while the next one loads (no flash on paging/typing). */
export const useDeploymentsQuery = (params: DeploymentListParams) =>
  useQuery({
    queryKey: deploymentKeys.page(params),
    queryFn: () => DeploymentApi.getDeployments(params),
    placeholderData: keepPreviousData,
  });
