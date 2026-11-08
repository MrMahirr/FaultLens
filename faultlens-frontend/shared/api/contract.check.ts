/**
 * Compile-time contract checks (P10 / 1.3). Nothing here runs; `pnpm typecheck` fails when a
 * hand-written DTO and the generated contract (./schema.d.ts, from contracts/openapi.json)
 * disagree:
 *  - field names: a field renamed, added or dropped on either side;
 *  - enum values: the backend added, renamed or dropped a value of an enum the UI switches on.
 *
 * The DTOs stay hand-written because they carry what the contract cannot: TS enums the UI is
 * built on, and which fields may be null (the OpenAPI document has no nullability).
 *
 * On a failure the error names the mismatching keys, e.g.
 *   Type '{ onlyInFrontend: "errorCount"; onlyInApi: "notes"; }' does not satisfy 'true'.
 * Fix the DTO (or the backend), then rerun `pnpm gen:api` if the contract changed.
 */
import type { ApiField, ApiSchema, SchemaName } from "./contract";
import type { ApiError, ApiResponse, PagedResponse } from "@/shared/types/api.types";
import type {
  AnalysisType,
  ConnectionStatus,
  LogSourceType,
  Severity,
} from "@/shared/types/common.types";
import type {
  GroupStatus,
  GroupStatusUpdate,
  LogDetailDto,
  LogEntryDto,
  LogGroupDto,
  LogGroupEventDto,
  LogGroupEventType,
  LogGroupCommentDto,
  AssignableUser,
  LogContextDto,
} from "@/features/logs/types/log.types";
import type {
  Alarm,
  AlarmChannel,
  AlarmRule,
  AlarmRuleInput,
  AlarmRuleType,
  AlarmStats,
  AlarmStatus,
} from "@/features/alarms/types/alarm.types";
import type { AnalysisResultDto } from "@/features/analyses/types/analysis.types";
import type {
  ApiKey,
  CreateApiKeyInput,
  CreatedApiKey,
} from "@/features/apiKeys/types/apiKey.types";
import type { DeploymentDto } from "@/features/deployments/types/deployment.types";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import type { CreateUserInput, ManagedUser } from "@/features/users/types/user.types";
import type { Role } from "@/features/auth/hooks/useCan";
import type {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
} from "@/features/auth/api/authApi";
import type { SystemSettingsDto } from "@/features/settings/api/useSystemSettings";

/* ── Helpers ───────────────────────────────────────────────── */

type Expect<T extends true> = T;

/** `true`, or the keys that differ (shown in the compiler error). */
type SameKeys<Frontend, N extends SchemaName> = [
  Exclude<keyof Frontend, keyof ApiSchema<N>>,
  Exclude<keyof ApiSchema<N>, keyof Frontend>,
] extends [never, never]
  ? true
  : {
      onlyInFrontend: Exclude<keyof Frontend, keyof ApiSchema<N>>;
      onlyInApi: Exclude<keyof ApiSchema<N>, keyof Frontend>;
    };

/** `true`, or the values that differ. `${E}` turns a string enum into its value union. */
type SameValues<Frontend extends string, Api> = [
  Exclude<Frontend, Api>,
  Exclude<Api, Frontend>,
] extends [never, never]
  ? true
  : { onlyInFrontend: Exclude<Frontend, Api>; onlyInApi: Exclude<Api, Frontend> };

type Item<T> = T extends readonly (infer I)[] ? I : never;

/* ── Field names ───────────────────────────────────────────── */

export type FieldChecks = [
  // Envelope and paging
  Expect<SameKeys<ApiResponse<unknown>, "ApiResponseString">>,
  Expect<SameKeys<PagedResponse<unknown>, "PagedResponseLogEntryDto">>,
  // Error envelope (every operation's `default` response): getApiErrorCode reads error.code.
  Expect<SameKeys<ApiError, "ErrorEnvelope">>,
  Expect<SameKeys<ApiError["error"], "ErrorBody">>,
  // Logs
  Expect<SameKeys<LogEntryDto, "LogEntryDto">>,
  Expect<SameKeys<LogGroupDto, "LogGroupDto">>,
  Expect<SameKeys<LogGroupEventDto, "LogGroupEventDto">>,
  Expect<SameKeys<GroupStatusUpdate, "GroupStatusUpdateRequest">>,
  Expect<SameKeys<LogGroupCommentDto, "LogGroupCommentDto">>,
  Expect<SameKeys<AssignableUser, "AssignableUser">>,
  Expect<SameKeys<LogContextDto, "LogContextDto">>,
  Expect<SameKeys<LogDetailDto, "LogDetailDto">>,
  // Alarms
  Expect<SameKeys<Alarm, "AlarmDto">>,
  Expect<SameKeys<AlarmStats, "AlarmStatsDto">>,
  Expect<SameKeys<AlarmRule, "AlarmRuleResponse">>,
  Expect<SameKeys<AlarmRuleInput, "AlarmRuleRequest">>,
  // Analyses, deployments, sources
  Expect<SameKeys<AnalysisResultDto, "AnalysisResultDto">>,
  Expect<SameKeys<DeploymentDto, "DeploymentDto">>,
  Expect<SameKeys<LogSourceDto, "LogSourceDto">>,
  // API keys
  Expect<SameKeys<ApiKey, "ApiKeyResponse">>,
  Expect<SameKeys<CreateApiKeyInput, "CreateApiKeyRequest">>,
  Expect<SameKeys<CreatedApiKey, "CreatedApiKeyResponse">>,
  // Users, auth, settings
  Expect<SameKeys<ManagedUser, "UserResponse">>,
  Expect<SameKeys<CreateUserInput, "CreateUserRequest">>,
  Expect<SameKeys<LoginRequest, "LoginRequest">>,
  Expect<SameKeys<LoginResponse, "TokenResponse">>,
  Expect<SameKeys<ChangePasswordRequest, "ChangePasswordRequest">>,
  Expect<SameKeys<SystemSettingsDto, "SystemSettingsResponse">>,
];

/* ── Enum values ───────────────────────────────────────────── */

export type EnumChecks = [
  Expect<SameValues<`${Severity}`, ApiField<"LogEntryDto", "severity">>>,
  Expect<SameValues<`${LogSourceType}`, ApiField<"LogSourceDto", "type">>>,
  Expect<SameValues<`${ConnectionStatus}`, ApiField<"LogSourceDto", "status">>>,
  Expect<SameValues<`${AlarmStatus}`, ApiField<"AlarmDto", "status">>>,
  Expect<SameValues<`${GroupStatus}`, ApiField<"LogGroupDto", "status">>>,
  Expect<SameValues<`${LogGroupEventType}`, ApiField<"LogGroupEventDto", "type">>>,
  Expect<SameValues<`${AlarmRuleType}`, ApiField<"AlarmRuleResponse", "type">>>,
  Expect<SameValues<`${AlarmChannel}`, Item<ApiField<"AlarmRuleResponse", "channels">>>>,
];

/**
 * Not checkable here: the backend declares these as plain strings, so the contract has no
 * value list. Role ("ADMIN" | "EDITOR" | "VIEWER"), AnalysisType (engine_type), ApiKeyScope,
 * DeploymentStatus and DeploymentEnvironment are validated by the backend and pinned by its
 * tests instead.
 */
export type UncheckedEnums = [Role, AnalysisType];
