import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { useTranslations } from "next-intl";
import { getApiErrorMessage } from "@/shared/api/errors";
import { AlarmApi } from "./alarmApi";
import { alarmKeys } from "./useAlarmsQuery";

/** Invalidates list pages and the stats counters together. */
type AlarmToast =
  | "acknowledged"
  | "acknowledgeFailed"
  | "resolved"
  | "resolveFailed"
  | "alarmDeleted"
  | "alarmDeleteFailed";

function useAlarmMutation(
  mutationFn: (id: number) => Promise<unknown>,
  successKey: AlarmToast,
  errorKey: AlarmToast,
) {
  const queryClient = useQueryClient();
  const t = useTranslations("alarms.toast");
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alarmKeys.all });
      toast.success(t(successKey));
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, t(errorKey)));
    },
  });
}

export const useAcknowledgeAlarmMutation = () =>
  useAlarmMutation(AlarmApi.acknowledge, "acknowledged", "acknowledgeFailed");

export const useResolveAlarmMutation = () =>
  useAlarmMutation(AlarmApi.resolve, "resolved", "resolveFailed");

export const useDeleteAlarmMutation = () =>
  useAlarmMutation(AlarmApi.remove, "alarmDeleted", "alarmDeleteFailed");
