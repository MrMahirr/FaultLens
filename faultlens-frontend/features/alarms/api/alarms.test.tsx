import { act, renderHook, waitFor } from "@/test/rtl";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Severity } from "@/shared/types/common.types";
import { API, fail, ok, server } from "@/test/msw/server";
import { createTestQueryClient, queryWrapper } from "@/test/utils";
import { AlarmStatus } from "../types/alarm.types";
import {
  useAcknowledgeAlarmMutation,
  useDeleteAlarmMutation,
  useResolveAlarmMutation,
} from "./useAlarmsMutation";
import { alarmKeys, useAlarmsQuery, useAlarmStatsQuery } from "./useAlarmsQuery";

/** One alarm exactly as the backend serialises it (see ApiContractIT). */
const backendAlarm = {
  id: 7,
  rule_id: "AI_ANALYSIS",
  rule_name: "Yapay Zeka Kritik Hata Tespiti",
  source_id: 3,
  severity: "CRITICAL",
  status: "ACTIVE",
  message: "Hata Analizi (Grup ID: 1)",
  triggered_at: "2026-11-04T10:00:00Z",
  resolved_at: null,
};

describe("alarm queries", () => {
  it("lists alarms with filters and paging, mapped to camelCase", async () => {
    let query: URLSearchParams | undefined;
    server.use(
      http.get(`${API}/alarms`, ({ request }) => {
        query = new URL(request.url).searchParams;
        return HttpResponse.json(
          ok({ content: [backendAlarm], page: 1, size: 10, total_elements: 11, total_pages: 2 }),
        );
      }),
    );

    const { result } = renderHook(
      () =>
        useAlarmsQuery({
          status: AlarmStatus.ACTIVE,
          severity: Severity.CRITICAL,
          page: 1,
          size: 10,
        }),
      { wrapper: queryWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(Object.fromEntries(query!)).toEqual({
      page: "1",
      size: "10",
      status: "ACTIVE",
      severity: "CRITICAL",
    });
    expect(result.current.data).toMatchObject({ page: 1, totalElements: 11, totalPages: 2 });
    expect(result.current.data?.content[0]).toEqual({
      id: 7,
      ruleId: "AI_ANALYSIS",
      ruleName: "Yapay Zeka Kritik Hata Tespiti",
      sourceId: 3,
      severity: "CRITICAL",
      status: "ACTIVE",
      message: "Hata Analizi (Grup ID: 1)",
      triggeredAt: "2026-11-04T10:00:00Z",
      resolvedAt: null,
    });
  });

  it("omits empty filters and defaults paging to 0/20", async () => {
    let query: URLSearchParams | undefined;
    server.use(
      http.get(`${API}/alarms`, ({ request }) => {
        query = new URL(request.url).searchParams;
        return HttpResponse.json(
          ok({ content: [], page: 0, size: 20, total_elements: 0, total_pages: 0 }),
        );
      }),
    );

    const { result } = renderHook(() => useAlarmsQuery({}), { wrapper: queryWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(Object.fromEntries(query!)).toEqual({ page: "0", size: "20" });
  });

  it("reads the stats counters", async () => {
    server.use(
      http.get(`${API}/alarms/stats`, () =>
        HttpResponse.json(ok({ active: 4, acknowledged: 2, resolved: 9, open_critical: 1 })),
      ),
    );

    const { result } = renderHook(() => useAlarmStatsQuery(), { wrapper: queryWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({
      active: 4,
      acknowledged: 2,
      resolved: 9,
      openCritical: 1,
    });
  });
});

describe("alarm mutations", () => {
  afterEach(() => vi.restoreAllMocks());

  it.each([
    ["acknowledge", useAcknowledgeAlarmMutation, "put", `${API}/alarms/7/ack`, "Alarm üstlenildi."],
    [
      "resolve",
      useResolveAlarmMutation,
      "put",
      `${API}/alarms/7/resolve`,
      "Alarm çözüldü olarak işaretlendi.",
    ],
    ["delete", useDeleteAlarmMutation, "delete", `${API}/alarms/7`, "Alarm başarıyla silindi."],
  ] as const)(
    "%s calls the endpoint, toasts and refreshes lists + stats",
    async (_, useHook, method, url, message) => {
      const success = vi.spyOn(toast, "success").mockImplementation(() => "");
      const called = vi.fn();
      server.use(
        http[method](url, () => {
          called();
          return method === "delete"
            ? new HttpResponse(null, { status: 204 })
            : HttpResponse.json(ok(backendAlarm));
        }),
      );
      const client = createTestQueryClient();
      const invalidate = vi.spyOn(client, "invalidateQueries");

      const { result } = renderHook(() => useHook(), { wrapper: queryWrapper(client) });
      await act(() => result.current.mutateAsync(7));

      expect(called).toHaveBeenCalledOnce();
      expect(success).toHaveBeenCalledWith(message);
      expect(invalidate).toHaveBeenCalledWith({ queryKey: alarmKeys.all });
    },
  );

  it("shows the backend error message when acknowledging a resolved alarm", async () => {
    const error = vi.spyOn(toast, "error").mockImplementation(() => "");
    server.use(
      http.put(`${API}/alarms/7/ack`, () =>
        HttpResponse.json(fail("ALARM_ALREADY_RESOLVED", "Alarm is already resolved"), {
          status: 400,
        }),
      ),
    );

    const { result } = renderHook(() => useAcknowledgeAlarmMutation(), { wrapper: queryWrapper() });
    await act(async () => {
      await result.current.mutateAsync(7).catch(() => {});
    });

    expect(error).toHaveBeenCalledWith("Alarm is already resolved");
  });

  it("falls back to the generic message on network errors", async () => {
    const error = vi.spyOn(toast, "error").mockImplementation(() => "");
    server.use(http.delete(`${API}/alarms/7`, () => HttpResponse.error()));

    const { result } = renderHook(() => useDeleteAlarmMutation(), { wrapper: queryWrapper() });
    await act(async () => {
      await result.current.mutateAsync(7).catch(() => {});
    });

    expect(error).toHaveBeenCalledWith("Alarm silinirken bir hata oluştu.");
  });
});
