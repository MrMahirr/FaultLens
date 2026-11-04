import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { API, fail, ok, server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/utils";
import { LoginForm } from "./LoginForm";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

/** Unsigned JWT whose payload carries the given subject and roles. */
function jwt(sub: string, roles: string[]) {
  const payload = Buffer.from(JSON.stringify({ sub, roles, exp: 9_999_999_999 })).toString("base64url");
  return `e30.${payload}.sig`;
}

async function submit(username: string, password: string) {
  const user = userEvent.setup();
  if (username) await user.type(screen.getByLabelText("Kullanıcı Adı"), username);
  if (password) await user.type(screen.getByLabelText("Şifre"), password);
  await user.click(screen.getByRole("button", { name: /giriş yap/i }));
}

describe("LoginForm", () => {
  beforeEach(() => {
    push.mockReset();
    useAuthStore.setState({ token: null, user: null });
  });

  it("validates the fields before calling the API", async () => {
    renderWithQuery(<LoginForm />);

    await submit("", "123");

    expect(await screen.findByText("Kullanıcı adı gereklidir")).toBeInTheDocument();
    expect(screen.getByText("Şifre en az 6 karakter olmalıdır")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("logs in, stores the session from the token and opens the dashboard", async () => {
    let sentBody: unknown;
    server.use(
      http.post(`${API}/auth/login`, async ({ request }) => {
        sentBody = await request.json();
        return HttpResponse.json(
          ok({ token: jwt("mahir", ["ROLE_ADMIN"]), expires_at: "2026-11-05T10:00:00Z", must_change_password: false }),
        );
      }),
    );
    renderWithQuery(<LoginForm />);

    await submit("mahir", "secret-pass");

    await waitFor(() => expect(push).toHaveBeenCalledWith("/dashboard"));
    expect(sentBody).toEqual({ username: "mahir", password: "secret-pass" });
    expect(useAuthStore.getState().user).toEqual({ username: "mahir", role: "ADMIN", mustChangePassword: false });
    expect(useAuthStore.getState().token).toBe(jwt("mahir", ["ROLE_ADMIN"]));
  });

  it("sends a user with a bootstrap password to /change-password", async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(ok({ token: jwt("admin", ["ADMIN"]), expires_at: "x", must_change_password: true })),
      ),
    );
    renderWithQuery(<LoginForm />);

    await submit("admin", "initial-pass");

    await waitFor(() => expect(push).toHaveBeenCalledWith("/change-password"));
    expect(useAuthStore.getState().user?.mustChangePassword).toBe(true);
  });

  it("shows the backend message on bad credentials and stays on the page", async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(fail("INVALID_CREDENTIALS", "Invalid username or password"), { status: 401 }),
      ),
    );
    renderWithQuery(<LoginForm />);

    await submit("admin", "wrong-pass");

    expect(await screen.findByText("Invalid username or password")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
    expect(useAuthStore.getState().token).toBeNull();
  });

  it("falls back to a Turkish message when the server sends none", async () => {
    server.use(http.post(`${API}/auth/login`, () => HttpResponse.error()));
    renderWithQuery(<LoginForm />);

    await submit("admin", "whatever-pass");

    expect(await screen.findByText("Geçersiz kullanıcı adı veya şifre")).toBeInTheDocument();
  });
});
