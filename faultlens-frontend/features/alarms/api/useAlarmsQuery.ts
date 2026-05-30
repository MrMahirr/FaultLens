import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { Alarm } from "../types/alarm.types";

export const useAlarmsQuery = () => {
  return useQuery({
    queryKey: ["alarms"],
    queryFn: async () => {
      const { data } = await apiClient.get<Alarm[]>("/api/alarms");
      // Map the string status and severity from backend to frontend Enums if needed, 
      // although they match the ENUM values directly.
      return data;
    },
    refetchInterval: 30000, // Fetch every 30 seconds
  });
};
