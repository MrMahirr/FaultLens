import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { API, fail, ok, server } from "@/test/msw/server";
import { fakeToken, signInAs } from "@/test/utils";
import { apiClient } from "./client";

const fresh = fakeToken("admin", "ADMIN");

beforeEach(() => {
  signInAs("ADMIN", "admin");
  useAuthStore.setState({ token: "expired-token" });
});

describe("401 → refresh → replay", () => {
  it("renews once for concurrent 401s and replays every request with the new token", async () => {
    let refreshes = 0;
    server.use(
      http.post(`${API}/auth/refresh`, () => {
        refreshes += 1;
        return HttpResponse.json(ok({ token: fresh, expires_at: "2099-01-01T00:00:00Z" }));
      }),
      http.get(`${API}/alarms`, ({ request }) =>
        request.headers.get("Authorization") === `Bearer ${fresh}`
          ? HttpResponse.json(ok([]))
          : HttpResponse.json(fail("UNAUTHORIZED", "expired"), { status: 401 }),
      ),
    );

    const results = await Promise.all([
      apiClient.get(`${API}/alarms`),
      apiClient.get(`${API}/alarms`),
      apiClient.get(`${API}/alarms`),
    ]);

    expect(results.map((r) => r.status)).toEqual([200, 200, 200]);
    expect(refreshes).toBe(1);
    expect(useAuthStore.getState().token).toBe(fresh);
    expect(useAuthStore.getState().user?.username).toBe("admin");
  });

  it("logs out when the refresh cookie is no longer valid", async () => {
    server.use(
      http.post(`${API}/auth/refresh`, () =>
        HttpResponse.json(fail("AUTH_FAILED", "Session expired"), { status: 401 }),
      ),
      http.get(`${API}/alarms`, () =>
        HttpResponse.json(fail("UNAUTHORIZED", "expired"), { status: 401 }),
      ),
    );

    await expect(apiClient.get(`${API}/alarms`)).rejects.toMatchObject({
      response: { status: 401 },
    });
    expect(useAuthStore.getState().token).toBeNull();
  });

  it("never tries to refresh a failed login", async () => {
    let refreshes = 0;
    server.use(
      http.post(`${API}/auth/refresh`, () => {
        refreshes += 1;
        return HttpResponse.json(ok({ token: fresh }));
      }),
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(fail("AUTH_FAILED", "Invalid username or password"), { status: 401 }),
      ),
    );

    await expect(apiClient.post(`${API}/auth/login`, {})).rejects.toBeDefined();
    expect(refreshes).toBe(0);
  });
});
