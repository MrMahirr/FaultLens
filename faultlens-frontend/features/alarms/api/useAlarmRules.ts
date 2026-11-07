import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { useTranslations } from "next-intl";
import { getApiErrorMessage } from "@/shared/api/errors";
import type { AlarmRuleInput } from "../types/alarm.types";
import { AlarmRuleApi } from "./alarmRuleApi";
import { alarmKeys } from "./useAlarmsQuery";

export const alarmRuleKeys = {
  all: [...alarmKeys.all, "rules"] as const,
};

export const useAlarmRulesQuery = () =>
  useQuery({ queryKey: alarmRuleKeys.all, queryFn: AlarmRuleApi.list });

type RuleToast =
  "created" | "createFailed" | "updated" | "updateFailed" | "deleted" | "deleteFailed";

function useRuleMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<unknown>,
  successKey: RuleToast,
  errorKey: RuleToast,
) {
  const queryClient = useQueryClient();
  const t = useTranslations("alarms.toast");
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alarmRuleKeys.all });
      toast.success(t(successKey));
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, t(errorKey)));
    },
  });
}

export const useCreateAlarmRuleMutation = () =>
  useRuleMutation((input: AlarmRuleInput) => AlarmRuleApi.create(input), "created", "createFailed");

export const useToggleAlarmRuleMutation = () =>
  useRuleMutation(
    ({ id, enabled }: { id: number; enabled: boolean }) => AlarmRuleApi.setEnabled(id, enabled),
    "updated",
    "updateFailed",
  );

export const useDeleteAlarmRuleMutation = () =>
  useRuleMutation((id: number) => AlarmRuleApi.remove(id), "deleted", "deleteFailed");
