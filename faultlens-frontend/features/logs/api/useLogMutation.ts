import { useMutation, useQueryClient } from "@tanstack/react-query";
import { LogApi } from "./logApi";
import { logKeys } from "./useLogQuery";

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
