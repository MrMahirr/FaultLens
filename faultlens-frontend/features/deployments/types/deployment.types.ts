import { DeploymentEnvironment, DeploymentStatus } from "@/shared/types/common.types";

export interface DeploymentDto {
  id: number;
  serviceName: string;
  version: string;
  environment: DeploymentEnvironment;
  status: DeploymentStatus;
  deployedBy: string;
  deployedAt: string;
  errorCount: number;
  description?: string;
}
