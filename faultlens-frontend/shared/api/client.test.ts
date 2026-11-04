import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { API, fail, ok, server } from "@/test/msw/server";
import { apiClient } from "./client";

describe("apiClient interceptors", () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null });
  });

  it("sends the stored token as a Bearer header", async () => {
    useAuthStore.setState({ token: "jwt-123", user: { username: "admin", role: "ADMIN" } });
    let authorization: string | null = null;
    server.use(
      http.get(`${API}/alarms/stats`, ({ request }) => {
        authorization = request.headers.get("authorization");
        return HttpResponse.json(ok({ active: 0 }));
      }),
    );

    await apiClient.get(`${API}/alarms/stats`);

    expect(authorization).toBe("Bearer jwt-123");
  });

  it("sends no Authorization header when logged out", async () => {
    let authorization: string | null = "unset";
    server.use(
      http.get(`${API}/alarms/stats`, ({ request }) => {
        authorization = request.headers.get("authorization");
        return HttpResponse.json(ok({}));
      }),
    );

    await apiClient.get(`${API}/alarms/stats`);

    expect(authorization).toBeNull();
  });

  it("converts request bodies to snake_case and responses to camelCase", async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/deployments`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          ok({ id: 1, service_name: "orders", deployed_at: "2026-11-04T10:00:00Z" }),
        );
      }),
    );

    const response = await apiClient.post(`${API}/deployments`, {
      serviceName: "orders",
      deployedBy: "ci",
    });

    expect(body).toEqual({ service_name: "orders", deployed_by: "ci" });
    expect(response.data.data).toEqual({
      id: 1,
      serviceName: "orders",
      deployedAt: "2026-11-04T10:00:00Z",
    });
  });

  it("logs out on 401", async () => {
    useAuthStore.setState({ token: "expired", user: { username: "admin", role: "ADMIN" } });
    server.use(
      http.get(`${API}/alarms`, () =>
        HttpResponse.json(fail("UNAUTHORIZED", "x"), { status: 401 }),
      ),
    );

    await expect(apiClient.get(`${API}/alarms`)).rejects.toMatchObject({
      response: { status: 401 },
    });

    expect(useAuthStore.getState().token).toBeNull();
    expect(useAuthStore.getState().user).toBeNull();
  });

  it("flags the user on 403 PASSWORD_CHANGE_REQUIRED without logging out", async () => {
    useAuthStore.setState({ token: "restricted", user: { username: "admin", role: "ADMIN" } });
    server.use(
      http.get(`${API}/alarms`, () =>
        HttpResponse.json(fail("PASSWORD_CHANGE_REQUIRED", "Password must be changed"), {
          status: 403,
        }),
      ),
    );

    await expect(apiClient.get(`${API}/alarms`)).rejects.toMatchObject({
      response: { status: 403 },
    });

    expect(useAuthStore.getState().token).toBe("restricted");
    expect(useAuthStore.getState().user?.mustChangePassword).toBe(true);
  });

  it("does not flag the user on other 403s", async () => {
    useAuthStore.setState({ token: "t", user: { username: "viewer", role: "VIEWER" } });
    server.use(
      http.get(`${API}/alarms`, () => HttpResponse.json(fail("FORBIDDEN", "no"), { status: 403 })),
    );

    await expect(apiClient.get(`${API}/alarms`)).rejects.toBeTruthy();

    expect(useAuthStore.getState().user?.mustChangePassword).toBeUndefined();
  });
});
