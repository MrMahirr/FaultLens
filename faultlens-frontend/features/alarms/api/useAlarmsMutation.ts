import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/client";
import { Alarm } from "../types/alarm.types";
import toast from "react-hot-toast";

export const useResolveAlarmMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string | number) => {
      const { data } = await apiClient.put<Alarm>(`/api/alarms/${id}/resolve`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["alarms"] });
      toast.success("Alarm çözüldü olarak işaretlendi.");
    },
    onError: (error) => {
      toast.error("Alarm çözülürken bir hata oluştu.");
      console.error(error);
    },
  });
};

export const useDeleteAlarmMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string | number) => {
      await apiClient.delete(`/api/alarms/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["alarms"] });
      toast.success("Alarm başarıyla silindi.");
    },
    onError: (error) => {
      toast.error("Alarm silinirken bir hata oluştu.");
      console.error(error);
    },
  });
};
