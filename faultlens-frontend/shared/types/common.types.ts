/**
 * Common enumerations used across the application.
 * These are domain-level types shared by all features.
 */

export enum Severity {
  TRACE = "TRACE",
  DEBUG = "DEBUG",
  INFO = "INFO",
  WARN = "WARN",
  ERROR = "ERROR",
  CRITICAL = "CRITICAL",
}

export enum LogSourceType {
  KUBERNETES = "KUBERNETES",
  SSH = "SSH",
  DOCKER = "DOCKER",
}

export enum AnalysisType {
  RULE_BASED = "RULE_BASED",
  AI_ANALYSIS = "AI_ANALYSIS",
}

export enum DeploymentStatus {
  SUCCESS = "SUCCESS",
  FAILED = "FAILED",
  ROLLED_BACK = "ROLLED_BACK",
  IN_PROGRESS = "IN_PROGRESS",
}

export enum ConnectionStatus {
  CONNECTED = "CONNECTED",
  WARNING = "WARNING",
  DISCONNECTED = "DISCONNECTED",
}

export enum DeploymentEnvironment {
  PRODUCTION = "PRODUCTION",
  STAGING = "STAGING",
  DEVELOPMENT = "DEVELOPMENT",
}

/**
 * Severity → CSS class mapping.
 * Component'larda kullanılır, renk bilgisi CSS'te tanımlıdır.
 */
export const SEVERITY_CLASS_MAP: Record<Severity, string> = {
  [Severity.TRACE]: "severity-trace",
  [Severity.DEBUG]: "severity-debug",
  [Severity.INFO]: "severity-info",
  [Severity.WARN]: "severity-warn",
  [Severity.ERROR]: "severity-error",
  [Severity.CRITICAL]: "severity-critical",
};

/**
 * Severity display labels
 */
export const SEVERITY_LABELS: Record<Severity, string> = {
  [Severity.TRACE]: "Trace",
  [Severity.DEBUG]: "Debug",
  [Severity.INFO]: "Info",
  [Severity.WARN]: "Warning",
  [Severity.ERROR]: "Error",
  [Severity.CRITICAL]: "Critical",
};

/**
 * Source type display labels
 */
export const SOURCE_TYPE_LABELS: Record<LogSourceType, string> = {
  [LogSourceType.KUBERNETES]: "Kubernetes",
  [LogSourceType.SSH]: "SSH",
  [LogSourceType.DOCKER]: "Docker",
};
