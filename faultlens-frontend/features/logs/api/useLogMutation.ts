import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LogApi } from "./logApi";
import { logKeys } from "./useLogQuery";
import type { GroupStatusUpdate } from "@/features/logs/types/log.types";

export const useClearLogsMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sourceId: number) => LogApi.clearLogs(sourceId),
    onSuccess: () => {
      // Invalidate all log queries so the UI updates to show empty list
      queryClient.invalidateQueries({ queryKey: logKeys.all });
    },
  });
};

/** Resolve / ignore / reopen. Refreshes every group list, the group and its timeline. */
export const useUpdateGroupStatusMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, update }: { id: number; update: GroupStatusUpdate }) =>
      LogApi.updateLogGroupStatus(id, update),
    onSuccess: (group) => {
      queryClient.setQueryData(logKeys.group(group.id), group);
      queryClient.invalidateQueries({ queryKey: logKeys.groups() });
    },
  });
};

/** Every group change refreshes the lists, the group, its timeline and comments. */
function useGroupMutation<TVars>(mutationFn: (vars: TVars) => Promise<unknown>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: logKeys.groups() }),
  });
}

export const useAssignGroupMutation = () =>
  useGroupMutation(({ id, assignee }: { id: number; assignee: string | null }) =>
    LogApi.assignLogGroup(id, assignee),
  );

export const useAddCommentMutation = () =>
  useGroupMutation(({ id, body }: { id: number; body: string }) =>
    LogApi.addLogGroupComment(id, body),
  );

export const useEditCommentMutation = () =>
  useGroupMutation(({ id, commentId, body }: { id: number; commentId: number; body: string }) =>
    LogApi.editLogGroupComment(id, commentId, body),
  );

export const useDeleteCommentMutation = () =>
  useGroupMutation(({ id, commentId }: { id: number; commentId: number }) =>
    LogApi.deleteLogGroupComment(id, commentId),
  );

export const useMergeGroupMutation = () =>
  useGroupMutation(({ id, targetId }: { id: number; targetId: number }) =>
    LogApi.mergeLogGroup(id, targetId),
  );

export const useUnmergeGroupMutation = () =>
  useGroupMutation(({ id }: { id: number }) => LogApi.unmergeLogGroup(id));
