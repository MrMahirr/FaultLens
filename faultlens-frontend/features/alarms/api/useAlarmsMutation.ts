import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { AlarmApi } from "./alarmApi";
import { alarmKeys } from "./useAlarmsQuery";

function apiErrorMessage(error: unknown, fallback: string): string {
  const apiError = (error as { response?: { data?: { error?: { message?: string } } } })
    ?.response?.data?.error;
  return apiError?.message ?? fallback;
}

/** Invalidates list pages and the stats counters together. */
function useAlarmMutation(
  mutationFn: (id: number) => Promise<unknown>,
  successMessage: string,
  errorMessage: string,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alarmKeys.all });
      toast.success(successMessage);
    },
    onError: (error) => {
      toast.error(apiErrorMessage(error, errorMessage));
    },
  });
}

export const useAcknowledgeAlarmMutation = () =>
  useAlarmMutation(AlarmApi.acknowledge, "Alarm üstlenildi.", "Alarm üstlenilemedi.");

export const useResolveAlarmMutation = () =>
  useAlarmMutation(AlarmApi.resolve, "Alarm çözüldü olarak işaretlendi.", "Alarm çözülürken bir hata oluştu.");

export const useDeleteAlarmMutation = () =>
  useAlarmMutation(AlarmApi.remove, "Alarm başarıyla silindi.", "Alarm silinirken bir hata oluştu.");
