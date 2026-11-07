import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { AlarmRule, AlarmRuleInput } from "../types/alarm.types";

/** Alarm rule REST client (/api/v1/alarms/rules). */
export const AlarmRuleApi = {
  list: async (): Promise<AlarmRule[]> => {
    const response = await apiClient({ method: HttpMethod.GET, url: Endpoints.ALARMS.RULES });
    return response.data.data;
  },

  create: async (input: AlarmRuleInput): Promise<AlarmRule> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.ALARMS.RULES,
      data: input,
    });
    return response.data.data;
  },

  update: async (id: number, input: AlarmRuleInput): Promise<AlarmRule> => {
    const response = await apiClient({
      method: HttpMethod.PUT,
      url: Endpoints.ALARMS.RULE(id),
      data: input,
    });
    return response.data.data;
  },

  setEnabled: async (id: number, enabled: boolean): Promise<AlarmRule> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: enabled ? Endpoints.ALARMS.RULE_ENABLE(id) : Endpoints.ALARMS.RULE_DISABLE(id),
    });
    return response.data.data;
  },

  remove: async (id: number): Promise<void> => {
    await apiClient({ method: HttpMethod.DELETE, url: Endpoints.ALARMS.RULE(id) });
  },
};
