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
  /** Collector health shared by all backend instances (P08). */
  health?: "CONNECTED" | "ERROR" | "IDLE";
  /** Last adapter error; kept after recovery for context. */
  lastError?: string | null;
  lastErrorAt?: string | null;
  /** Backend instance collecting (or last collecting) this source. */
  collectorInstance?: string | null;
}
