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
