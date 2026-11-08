import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type {
  LogEntryDto,
  LogFilters,
  LogGroupDto,
  LogGroupEventDto,
  LogGroupCommentDto,
  AssignableUser,
  LogGroupListParams,
  GroupStatusUpdate,
  LogDetailDto,
} from "@/features/logs/types/log.types";
import { DEFAULT_LOG_TIME_RANGE, rangeStart } from "@/features/logs/types/log.types";
import type { PagedResponse } from "@/shared/types/api.types";

export const LogApi = {
  getLogs: async (filters: LogFilters): Promise<PagedResponse<LogEntryDto>> => {
    const params: Record<string, string | number> = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };

    if (filters.severity) {
      params.severity = filters.severity;
    }
    if (filters.search) {
      params.search = filters.search;
    }
    if (filters.source) {
      const sourceIdNum = Number(filters.source);
      if (!isNaN(sourceIdNum)) {
        params.sourceId = sourceIdNum;
      }
    }
    // Always bounded by time (default: last 24 hours), computed at request time.
    params.from = filters.startDate ?? rangeStart(filters.range ?? DEFAULT_LOG_TIME_RANGE);
    if (filters.endDate) {
      params.to = filters.endDate;
    }

    const url = filters.groupId
      ? Endpoints.LOGS.GROUP_ENTRIES(filters.groupId)
      : Endpoints.LOGS.LIST;

    const response = await apiClient({
      method: HttpMethod.GET,
      url,
      params,
    });

    return response.data.data;
  },

  getLogDetail: async (id: number): Promise<LogDetailDto> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.DETAIL(id),
    });
    return response.data.data;
  },

  /** Without statuses the API leaves IGNORED groups out. */
  getLogGroups: async (params: LogGroupListParams = {}): Promise<PagedResponse<LogGroupDto>> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.GROUPS,
      params: {
        // Comma-separated: axios would send an array as status[]=…, which Spring does not bind.
        ...(params.statuses?.length ? { status: params.statuses.join(",") } : {}),
        ...(params.severity ? { severity: params.severity } : {}),
        ...(params.assignee ? { assignee: params.assignee } : {}),
        ...(params.page !== undefined ? { page: params.page } : {}),
        ...(params.size !== undefined ? { size: params.size } : {}),
      },
    });
    return response.data.data;
  },

  getLogGroup: async (id: number): Promise<LogGroupDto> => {
    const response = await apiClient({ method: HttpMethod.GET, url: Endpoints.LOGS.GROUP(id) });
    return response.data.data;
  },

  getLogGroupEvents: async (id: number): Promise<LogGroupEventDto[]> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.GROUP_EVENTS(id),
    });
    return response.data.data;
  },

  updateLogGroupStatus: async (id: number, update: GroupStatusUpdate): Promise<LogGroupDto> => {
    const response = await apiClient({
      method: HttpMethod.PATCH,
      url: Endpoints.LOGS.GROUP(id),
      data: update,
    });
    return response.data.data;
  },

  getAssignableUsers: async (): Promise<AssignableUser[]> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.GROUP_ASSIGNEES,
    });
    return response.data.data;
  },

  /** null unassigns. */
  assignLogGroup: async (id: number, assignee: string | null): Promise<LogGroupDto> => {
    const response = await apiClient({
      method: HttpMethod.PUT,
      url: Endpoints.LOGS.GROUP_ASSIGNEE(id),
      data: { assignee },
    });
    return response.data.data;
  },

  getLogGroupComments: async (id: number): Promise<LogGroupCommentDto[]> => {
    const response = await apiClient({
      method: HttpMethod.GET,
      url: Endpoints.LOGS.GROUP_COMMENTS(id),
    });
    return response.data.data;
  },

  addLogGroupComment: async (id: number, body: string): Promise<LogGroupCommentDto> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.LOGS.GROUP_COMMENTS(id),
      data: { body },
    });
    return response.data.data;
  },

  editLogGroupComment: async (
    id: number,
    commentId: number,
    body: string,
  ): Promise<LogGroupCommentDto> => {
    const response = await apiClient({
      method: HttpMethod.PUT,
      url: Endpoints.LOGS.GROUP_COMMENT(id, commentId),
      data: { body },
    });
    return response.data.data;
  },

  deleteLogGroupComment: async (id: number, commentId: number): Promise<void> => {
    await apiClient({
      method: HttpMethod.DELETE,
      url: Endpoints.LOGS.GROUP_COMMENT(id, commentId),
    });
  },

  mergeLogGroup: async (id: number, targetId: number): Promise<LogGroupDto> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.LOGS.GROUP_MERGE(id),
      data: { targetId },
    });
    return response.data.data;
  },

  unmergeLogGroup: async (id: number): Promise<LogGroupDto> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.LOGS.GROUP_UNMERGE(id),
    });
    return response.data.data;
  },

  clearLogs: async (sourceId: number): Promise<void> => {
    await apiClient({
      method: HttpMethod.DELETE,
      url: `${Endpoints.LOGS.LIST}/clear`,
      params: { sourceId },
    });
  },
};
