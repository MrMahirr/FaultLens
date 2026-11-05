import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { AlarmRuleInput } from "../types/alarm.types";
import { AlarmRuleApi } from "./alarmRuleApi";
import { alarmKeys } from "./useAlarmsQuery";

export const alarmRuleKeys = {
  all: [...alarmKeys.all, "rules"] as const,
};

export const useAlarmRulesQuery = () =>
  useQuery({ queryKey: alarmRuleKeys.all, queryFn: AlarmRuleApi.list });

function useRuleMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<unknown>,
  successMessage: string,
  errorMessage: string,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alarmRuleKeys.all });
      toast.success(successMessage);
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, errorMessage));
    },
  });
}

export const useCreateAlarmRuleMutation = () =>
  useRuleMutation(
    (input: AlarmRuleInput) => AlarmRuleApi.create(input),
    "Alarm kuralı oluşturuldu.",
    "Kural oluşturulamadı.",
  );

export const useToggleAlarmRuleMutation = () =>
  useRuleMutation(
    ({ id, enabled }: { id: number; enabled: boolean }) => AlarmRuleApi.setEnabled(id, enabled),
    "Kural güncellendi.",
    "Kural güncellenemedi.",
  );

export const useDeleteAlarmRuleMutation = () =>
  useRuleMutation((id: number) => AlarmRuleApi.remove(id), "Kural silindi.", "Kural silinemedi.");
