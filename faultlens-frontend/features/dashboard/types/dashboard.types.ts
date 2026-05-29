export interface DashboardStats {
  totalErrors: number;
  totalErrorsChange: number;
  activeSources: number;
  totalSources: number;
  openGroups: number;
  analysesToday: number;
}

export interface ChartPoint {
  time: string;
  critical: number;
  error: number;
  warn: number;
  info: number;
}
