import { render, screen, waitFor } from "@/test/rtl";
import { http, HttpResponse } from "msw";
import { API, fail, ok, server } from "@/test/msw/server";
import { fakeToken } from "@/test/utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/features/auth/store/auth.store";
import DashboardLayout from "./layout";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, push: vi.fn() }) }));
// The chrome around the page is not under test (and opens WebSockets).
vi.mock("@/shared/components/layout/Sidebar", () => ({ Sidebar: () => null }));
vi.mock("@/shared/components/layout/Topbar", () => ({ Topbar: () => null }));
vi.mock("@/shared/components/layout/GlobalNotificationListener", () => ({
  GlobalNotificationListener: () => null,
}));

function renderLayout() {
  return render(
    <DashboardLayout>
      <p>page content</p>
    </DashboardLayout>,
  );
}

describe("DashboardLayout", () => {
  beforeEach(() => {
    replace.mockReset();
    useAuthStore.setState({ token: null, user: null });
  });
  afterEach(() => vi.restoreAllMocks());

  it("does not redirect before the persisted session has been read", () => {
    // Regression: a hard load of /logs saw the initial (empty) state, went to /login and
    // from there to /dashboard, so deep links and refreshes never stayed on the page.
    vi.spyOn(useAuthStore.persist, "hasHydrated").mockReturnValue(false);

    renderLayout();

    expect(replace).not.toHaveBeenCalled();
    expect(screen.queryByText("page content")).not.toBeInTheDocument();
  });

  it("sends a visitor without a session to /login", () => {
    renderLayout();

    expect(replace).toHaveBeenCalledWith("/login");
    expect(screen.queryByText("page content")).not.toBeInTheDocument();
  });

  it("sends a user with a pending password change to /change-password", () => {
    useAuthStore.setState({
      token: "t",
      user: { username: "admin", role: "ADMIN", mustChangePassword: true },
    });

    renderLayout();

    expect(replace).toHaveBeenCalledWith("/change-password");
  });

  it("renders the page for a signed-in user without redirecting", () => {
    useAuthStore.setState({ token: "t", user: { username: "admin", role: "ADMIN" } });

    renderLayout();

    expect(screen.getByText("page content")).toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("restores the in-memory token from the refresh cookie after a reload", async () => {
    const fresh = fakeToken("admin", "ADMIN");
    server.use(http.post(`${API}/auth/refresh`, () => HttpResponse.json(ok({ token: fresh }))));
    useAuthStore.setState({ token: null, user: { username: "admin", role: "ADMIN" } });

    renderLayout();

    expect(await screen.findByText("page content")).toBeInTheDocument();
    expect(useAuthStore.getState().token).toBe(fresh);
    expect(replace).not.toHaveBeenCalled();
  });

  it("ends at /login when the session cannot be restored", async () => {
    server.use(
      http.post(`${API}/auth/refresh`, () =>
        HttpResponse.json(fail("AUTH_FAILED", "Session expired"), { status: 401 }),
      ),
    );
    useAuthStore.setState({ token: null, user: { username: "admin", role: "ADMIN" } });

    renderLayout();

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(useAuthStore.getState().user).toBeNull();
  });
});
