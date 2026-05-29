import { ConnectionStatus, LogSourceType } from "@/shared/types/common.types";

export interface LogSourceDto {
  id: number;
  name: string;
  type: LogSourceType;
  config: string;
  status?: ConnectionStatus;
  enabled: boolean;
  lastSeenAt: string;
  createdAt: string;
}
