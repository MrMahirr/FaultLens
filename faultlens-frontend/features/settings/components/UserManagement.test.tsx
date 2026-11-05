import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import Swal from "sweetalert2";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { UserManagement } from "./UserManagement";

vi.mock("sweetalert2", () => ({ default: { fire: vi.fn() } }));

const users = [
  {
    id: 1,
    username: "admin",
    email: null,
    role: "ADMIN",
    must_change_password: false,
    created_at: null,
  },
  {
    id: 2,
    username: "ayse",
    email: "ayse@example.com",
    role: "VIEWER",
    must_change_password: true,
    created_at: null,
  },
];

beforeEach(() => {
  signInAs("ADMIN", "admin");
  vi.mocked(Swal.fire).mockReset();
  vi.spyOn(toast, "success").mockImplementation(() => "");
  vi.spyOn(toast, "error").mockImplementation(() => "");
  server.use(http.get(`${API}/users`, () => HttpResponse.json(ok(users))));
});

describe("UserManagement", () => {
  it("lists users and offers no self-destructive actions on the own row", async () => {
    renderWithQuery(<UserManagement />);

    const own = (await screen.findByText("admin")).closest("tr")!;
    expect(within(own).queryByRole("button", { name: "Sil" })).not.toBeInTheDocument();
    expect(within(own).queryByRole("combobox")).not.toBeInTheDocument();

    const other = screen.getByText("ayse").closest("tr")!;
    expect(within(other).getByText("Şifre değişikliği bekliyor")).toBeInTheDocument();
    expect(within(other).getByRole("button", { name: "Sil" })).toBeInTheDocument();
  });

  it("changes a role", async () => {
    let body: unknown;
    server.use(
      http.put(`${API}/users/2/role`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(ok({ ...users[1], role: "EDITOR" }));
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<UserManagement />);

    await user.selectOptions(await screen.findByLabelText("ayse rolü"), "EDITOR");
    await waitFor(() => expect(body).toEqual({ role: "EDITOR" }));
  });

  it("creates a user after validating the form", async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/users`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(ok({ ...users[1], id: 3, username: "mehmet" }), { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<UserManagement />);
    await screen.findByText("ayse");

    await user.type(screen.getByLabelText("Kullanıcı Adı"), "mehmet");
    await user.type(screen.getByLabelText("Geçici Şifre"), "short");
    await user.click(screen.getByRole("button", { name: /kullanıcı ekle/i }));
    expect(await screen.findByText("Şifre en az 8 karakter olmalı")).toBeInTheDocument();
    expect(body).toBeUndefined();

    await user.type(screen.getByLabelText("Geçici Şifre"), "-enough");
    await user.selectOptions(screen.getByLabelText("Rol"), "EDITOR");
    await user.click(screen.getByRole("button", { name: /kullanıcı ekle/i }));
    await waitFor(() =>
      expect(body).toEqual({ username: "mehmet", role: "EDITOR", password: "short-enough" }),
    );
  });
});
