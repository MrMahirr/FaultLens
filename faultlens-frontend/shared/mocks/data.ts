/**
 * Mock data for the entire application.
 * Bu dosya backend olmadığında geliştirme sırasında kullanılır.
 * Her feature kendi mock data'sını bu dosyadan import eder.
 */

import {
  Severity,
  LogSourceType,
  AnalysisType,
  DeploymentStatus,
  ConnectionStatus,
  DeploymentEnvironment,
} from "@/shared/types/common.types";

// ── Log Entries ─────────────────────────────────────────────

export interface MockLogEntry {
  id: number;
  sourceId: number;
  groupId?: number;
  severity: Severity;
  message: string;
  parsedMessage?: string;
  stackTrace?: string;
  namespace?: string;
  podName?: string;
  containerName?: string;
  serviceName?: string;
  cluster?: string;
  timestamp: string;
}

const now = new Date();
const minutesAgo = (min: number) =>
  new Date(now.getTime() - min * 60 * 1000).toISOString();

export const mockLogEntries: MockLogEntry[] = [
  {
    id: 1,
    severity: Severity.CRITICAL,
    message: "OutOfMemoryError: Java heap space — unable to allocate 256MB",
    sourceId: 1,
    namespace: "payments",
    timestamp: minutesAgo(2),
    stackTrace: `java.lang.OutOfMemoryError: Java heap space
    at java.util.Arrays.copyOf(Arrays.java:3210)
    at com.payment.service.TransactionProcessor.processLargePayload(TransactionProcessor.java:89)
    at com.payment.controller.PaymentController.handlePayment(PaymentController.java:42)`,
    groupId: 1,
    podName: "payment-service-7d9f8b6c4-xk2lp",
    containerName: "payment-api",
  },
  {
    id: 2,
    severity: Severity.ERROR,
    message: "NullPointerException at PaymentService.java:142",
    sourceId: 1,
    namespace: "payments",
    timestamp: minutesAgo(5),
    stackTrace: `java.lang.NullPointerException
    at com.payment.service.PaymentService.validateOrder(PaymentService.java:142)
    at com.payment.service.PaymentService.processPayment(PaymentService.java:98)
    at com.payment.controller.PaymentController.create(PaymentController.java:55)`,
    groupId: 2,
    podName: "payment-service-7d9f8b6c4-xk2lp",
    containerName: "payment-api",
  },
  {
    id: 3,
    severity: Severity.ERROR,
    message: "Connection refused: db-primary:5432 — max connections reached",
    sourceId: 1,
    namespace: "database",
    timestamp: minutesAgo(8),
    groupId: 3,
  },
  {
    id: 4,
    severity: Severity.WARN,
    message: "Connection pool exhausted, waiting for available connection (timeout: 30s)",
    sourceId: 1,
    namespace: "payments",
    timestamp: minutesAgo(10),
    groupId: 3,
    podName: "payment-service-7d9f8b6c4-abc12",
    containerName: "payment-api",
  },
  {
    id: 5,
    severity: Severity.WARN,
    message: "Slow query detected: SELECT * FROM orders WHERE status='pending' (2340ms)",
    sourceId: 1,
    namespace: "orders",
    timestamp: minutesAgo(12),
    podName: "order-service-5c8d7b3a1-mn4op",
    containerName: "order-api",
  },
  {
    id: 6,
    severity: Severity.INFO,
    message: "Pod payment-service-7d9f8b6c4-xk2lp restarted successfully (restart count: 3)",
    sourceId: 1,
    namespace: "payments",
    timestamp: minutesAgo(15),
    podName: "payment-service-7d9f8b6c4-xk2lp",
    containerName: "payment-api",
  },
  {
    id: 7,
    severity: Severity.ERROR,
    message: "TLS handshake failed: certificate expired for api.external-service.com",
    sourceId: 1,
    namespace: "gateway",
    timestamp: minutesAgo(18),
    groupId: 4,
  },
  {
    id: 8,
    severity: Severity.INFO,
    message: "Deployment v2.3.1 rolled out successfully to 3/3 pods",
    sourceId: 1,
    namespace: "payments",
    timestamp: minutesAgo(22),
    podName: "payment-service-7d9f8b6c4-xk2lp",
    containerName: "payment-api",
  },
  {
    id: 9,
    severity: Severity.DEBUG,
    message: "Cache hit ratio: 94.2% — Redis cluster healthy",
    sourceId: 1,
    namespace: "cache",
    timestamp: minutesAgo(25),
    podName: "redis-cluster-0",
    containerName: "redis",
  },
  {
    id: 10,
    severity: Severity.TRACE,
    message: "HTTP GET /api/v1/health → 200 OK (12ms)",
    sourceId: 1,
    namespace: "monitoring",
    timestamp: minutesAgo(28),
  },
  {
    id: 11,
    severity: Severity.CRITICAL,
    message: "Disk usage at 95% on node worker-3 — immediate action required",
    sourceId: 1,
    namespace: "infrastructure",
    timestamp: minutesAgo(3),
    groupId: 5,
    podName: "node-exporter-worker-3",
    containerName: "node-exporter",
  },
  {
    id: 12,
    severity: Severity.ERROR,
    message: "Failed to pull image: registry.internal.com/auth-service:v1.4.2 — unauthorized",
    sourceId: 1,
    namespace: "auth",
    timestamp: minutesAgo(35),
    podName: "auth-service-deployment-6f7g8h-ab1cd",
    containerName: "auth-api",
  },
];

// ── Log Groups ──────────────────────────────────────────────

export interface MockLogGroup {
  id: number;
  fingerprint: string;
  firstMessage: string;
  count: number;
  severity: Severity;
  firstSeenAt: string;
  lastSeenAt: string;
}

export const mockLogGroups: MockLogGroup[] = [
  {
    id: 1,
    fingerprint: "oom-java-heap",
    firstMessage: "OutOfMemoryError: Java heap space",
    count: 23,
    severity: Severity.CRITICAL,
    firstSeenAt: minutesAgo(180),
    lastSeenAt: minutesAgo(2),
  },
  {
    id: 2,
    fingerprint: "npe-payment-service",
    firstMessage: "NullPointerException at PaymentService.java:142",
    count: 47,
    severity: Severity.ERROR,
    firstSeenAt: minutesAgo(360),
    lastSeenAt: minutesAgo(5),
  },
  {
    id: 3,
    fingerprint: "db-connection-refused",
    firstMessage: "Connection refused: db-primary:5432",
    count: 12,
    severity: Severity.ERROR,
    firstSeenAt: minutesAgo(120),
    lastSeenAt: minutesAgo(8),
  },
  {
    id: 4,
    fingerprint: "tls-cert-expired",
    firstMessage: "TLS handshake failed: certificate expired",
    count: 8,
    severity: Severity.ERROR,
    firstSeenAt: minutesAgo(60),
    lastSeenAt: minutesAgo(18),
  },
  {
    id: 5,
    fingerprint: "disk-usage-critical",
    firstMessage: "Disk usage at 95% on node worker-3",
    count: 5,
    severity: Severity.CRITICAL,
    firstSeenAt: minutesAgo(30),
    lastSeenAt: minutesAgo(3),
  },
];

// ── Sources ─────────────────────────────────────────────────

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

export const mockSources: LogSourceDto[] = [
  {
    id: 1,
    name: "prod-k8s-cluster",
    type: LogSourceType.KUBERNETES,
    config: JSON.stringify({ namespace: "payments" }),
    status: ConnectionStatus.CONNECTED,
    enabled: true,
    lastSeenAt: minutesAgo(2),
    createdAt: "2024-01-01T00:00:00Z",
  },
  {
    id: 2,
    name: "staging-ssh",
    type: LogSourceType.SSH,
    config: JSON.stringify({ host: "staging.example.com:22" }),
    status: ConnectionStatus.CONNECTED,
    enabled: true,
    lastSeenAt: minutesAgo(8),
    createdAt: "2024-01-15T00:00:00Z",
  },
  {
    id: 3,
    name: "prod-docker",
    type: LogSourceType.DOCKER,
    config: JSON.stringify({ host: "docker.prod.internal:2375" }),
    status: ConnectionStatus.WARNING,
    enabled: true,
    lastSeenAt: minutesAgo(18),
    createdAt: "2024-02-01T00:00:00Z",
  },
  {
    id: 4,
    name: "dev-k8s-local",
    type: LogSourceType.KUBERNETES,
    config: JSON.stringify({ namespace: "default" }),
    status: ConnectionStatus.DISCONNECTED,
    enabled: false,
    lastSeenAt: minutesAgo(1440),
    createdAt: "2024-03-01T00:00:00Z",
  },
];

// ── Analyses ────────────────────────────────────────────────

export interface AnalysisResultDto {
  id: number;
  logGroupId?: number;
  logEntryId?: number;
  rootCause: string;
  suggestion: string;
  affectedDeployment?: string;
  confidenceScore: number;
  engineType: AnalysisType;
  analyzedAt: string;
};
  createdAt: string;
}

export const mockAnalyses: AnalysisResultDto[] = [
  {
    id: 1,
    logGroupId: 1,
    engineType: AnalysisType.RULE_BASED,
    rootCause: "JVM heap alanı yetersiz. TransactionProcessor.processLargePayload() metodu büyük payload'ları memory'de tutarak heap tüketimini artırıyor. Default heap size (256MB) büyük transaction'lar için yetersiz.",
    suggestion: "1. JVM heap size'ı artırın: -Xmx512m veya -Xmx1g\n2. TransactionProcessor.processLargePayload() metodunda streaming işleme kullanın\n3. PaymentService.java:89 satırını inceleyin — büyük array kopyalama tespit edildi",
    confidenceScore: 0.92,
    affectedDeployment: "payment-service:v2.3.1",
    analyzedAt: minutesAgo(30),
  },
  {
    id: 2,
    groupId: 2,
    groupMessage: "NullPointerException at PaymentService.java:142",
    type: AnalysisType.RULE_BASED,
    rootCause:
      "PaymentService.validateOrder() metodunda null referans erişimi. Order nesnesi initialize edilmeden kullanılıyor. validateOrder() parametresi null kontrolünden geçirilmemiş.",
    suggestion:
      "1. PaymentService.java:142 satırında null check ekleyin\n2. Optional<Order> kullanımına geçin\n3. @NotNull annotation ekleyin",
    confidence: 0.88,
    createdAt: minutesAgo(120),
  },
  {
    id: 3,
    logGroupId: 5,
    engineType: AnalysisType.AI_ANALYSIS,
    rootCause: "Worker-3 node'unda log dosyaları ve container image cache'i disk alanını doldurmuş. /var/log ve /var/lib/docker dizinleri toplam 180GB kullanıyor.",
    suggestion: "1. Log rotation politikası uygulayın (max 7 gün)\n2. Kullanılmayan Docker image'ları temizleyin: docker system prune\n3. PersistentVolume kullanarak log depolamayı ayırın",
    confidenceScore: 0.78,
    affectedDeployment: "payment-service:v2.3.1",
    analyzedAt: minutesAgo(15),
  },
];

// ── Deployments ─────────────────────────────────────────────

export interface MockDeployment {
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

export const mockDeployments: MockDeployment[] = [
  {
    id: 1,
    serviceName: "payment-service",
    version: "v2.3.1",
    environment: DeploymentEnvironment.PRODUCTION,
    status: DeploymentStatus.SUCCESS,
    deployedBy: "ci-bot",
    deployedAt: minutesAgo(240),
    errorCount: 23,
    description: "Fixed transaction timeout issue, increased batch size",
  },
  {
    id: 2,
    serviceName: "node-exporter",
    version: "v1.7.0",
    environment: DeploymentEnvironment.PRODUCTION,
    status: DeploymentStatus.SUCCESS,
    deployedBy: "infra-team",
    deployedAt: minutesAgo(480),
    errorCount: 5,
    description: "Updated monitoring agent to latest version",
  },
  {
    id: 3,
    serviceName: "auth-service",
    version: "v1.4.2",
    environment: DeploymentEnvironment.STAGING,
    status: DeploymentStatus.FAILED,
    deployedBy: "developer-a",
    deployedAt: minutesAgo(720),
    errorCount: 0,
    description: "New OAuth2 integration — image pull failed",
  },
  {
    id: 4,
    serviceName: "order-service",
    version: "v3.1.0",
    environment: DeploymentEnvironment.PRODUCTION,
    status: DeploymentStatus.ROLLED_BACK,
    deployedBy: "ci-bot",
    deployedAt: minutesAgo(1440),
    errorCount: 15,
    description: "Slow query regression detected, rolled back",
  },
  {
    id: 5,
    serviceName: "payment-service",
    version: "v2.3.0",
    environment: DeploymentEnvironment.PRODUCTION,
    status: DeploymentStatus.SUCCESS,
    deployedBy: "ci-bot",
    deployedAt: minutesAgo(2880),
    errorCount: 0,
    description: "Stable release with minor performance improvements",
  },
];

// ── Dashboard Stats ─────────────────────────────────────────

export interface MockDashboardStats {
  totalErrors: number;
  totalErrorsChange: number;
  activeSources: number;
  totalSources: number;
  openGroups: number;
  analysesToday: number;
}

export const mockDashboardStats: MockDashboardStats = {
  totalErrors: 142,
  totalErrorsChange: 12.5,
  activeSources: 3,
  totalSources: 4,
  openGroups: 5,
  analysesToday: 3,
};

// ── Severity Chart Data ─────────────────────────────────────

export interface MockChartPoint {
  time: string;
  critical: number;
  error: number;
  warn: number;
  info: number;
}

export const mockSeverityChartData: MockChartPoint[] = Array.from(
  { length: 12 },
  (_, i) => ({
    time: `${60 - i * 5}m`,
    critical: Math.floor(Math.random() * 3),
    error: Math.floor(Math.random() * 15) + 2,
    warn: Math.floor(Math.random() * 25) + 5,
    info: Math.floor(Math.random() * 40) + 10,
  })
).reverse();
