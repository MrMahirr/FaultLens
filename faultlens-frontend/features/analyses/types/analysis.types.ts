import { AnalysisType } from "@/shared/types/common.types";

export interface AnalysisResultDto {
  id: number;
  logGroupId?: number;
  logEntryId?: number;
  sourceId?: number;
  rootCause: string;
  suggestion: string;
  affectedDeployment?: string;
  confidenceScore: number;
  engineType: AnalysisType;
  analyzedAt: string;
}
