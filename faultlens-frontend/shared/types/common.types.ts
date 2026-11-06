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
  CICD = "CICD",
  LOCAL_FILE = "LOCAL_FILE",
}

export enum AnalysisType {
  RULE_BASED = "RULE_BASED",
  /** Backend AiAnalysisEngine stores engine_type = "AI" */
  AI = "AI",
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

// Labels for these enums live in the message catalogs (messages/<locale>/*.json);
// enum values themselves (ERROR, KUBERNETES …) are shown untranslated.
