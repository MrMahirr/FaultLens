import { screen, waitFor, within } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { ApiKeySettings } from "./ApiKeySettings";

const TOKEN = "flk_Ab12Cd34_0123456789abcdefghijABCDEFGHIJkl";

const stored = {
  id: 3,
  name: "github-actions",
  display_key: "flk_Ab12Cd34_…",
  scopes: ["deployments:write"],
  source_id: null,
  created_by: "admin",
  created_at: "2026-11-01T10:00:00Z",
  expires_at: null,
  last_used_at: null,
  last_used_ip: null,
  revoked_at: null,
  active: true,
};

let keys: Array<Omit<typeof stored, "revoked_at"> & { revoked_at: string | null }>;

beforeEach(() => {
  signInAs("ADMIN", "admin");
  keys = [];
  vi.spyOn(toast, "success").mockImplementation(() => "");
  vi.spyOn(toast, "error").mockImplementation(() => "");
  server.use(
    http.get(`${API}/api-keys`, () => HttpResponse.json(ok(keys))),
    http.get(`${API}/sources`, () =>
      HttpResponse.json(
        ok([
          { id: 4, name: "orders-http", type: "HTTP", config: "{}", enabled: true },
          { id: 5, name: "k8s", type: "KUBERNETES", config: "{}", enabled: true },
        ]),
      ),
    ),
  );
});

describe("ApiKeySettings", () => {
  it("creates a key with the chosen scopes and shows the token exactly once", async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/api-keys`, async ({ request }) => {
        body = await request.json();
        keys = [{ ...stored, id: 9, name: "ci-prod", scopes: ["deployments:write", "logs:read"] }];
        return HttpResponse.json(ok({ key: keys[0], token: TOKEN }), { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<ApiKeySettings />);

    await user.click(await screen.findByRole("button", { name: "Yeni anahtar" }));
    const form = screen.getByRole("dialog", { name: "Yeni API anahtarı" });
    await user.type(within(form).getByLabelText("Ad"), "ci-prod");
    await user.click(within(form).getByRole("checkbox", { name: /logs:read/ }));
    await user.selectOptions(within(form).getByLabelText("Geçerlilik"), "90");
    await user.click(within(form).getByRole("button", { name: "Anahtarı oluştur" }));

    await waitFor(() =>
      expect(body).toEqual({
        name: "ci-prod",
        scopes: ["deployments:write", "logs:read"],
        expires_in_days: 90,
      }),
    );
    const shown = await screen.findByRole("dialog", { name: "Anahtar oluşturuldu" });
    expect(within(shown).getByTestId("api-key-token")).toHaveTextContent(TOKEN);

    await user.click(within(shown).getByRole("button", { name: "Kopyaladım, kapat" }));
    await waitFor(() => expect(screen.queryByTestId("api-key-token")).not.toBeInTheDocument());
    // The list only ever shows the masked key.
    expect(await screen.findByText("ci-prod")).toBeInTheDocument();
    expect(screen.queryByText(TOKEN)).not.toBeInTheDocument();
  });

  it("does not submit without a scope", async () => {
    const create = vi.fn();
    server.use(http.post(`${API}/api-keys`, create));
    const user = userEvent.setup();
    renderWithQuery(<ApiKeySettings />);

    await user.click(await screen.findByRole("button", { name: "Yeni anahtar" }));
    const form = screen.getByRole("dialog");
    await user.type(within(form).getByLabelText("Ad"), "x");
    await user.click(within(form).getByRole("checkbox", { name: /deployments:write/ }));
    await user.click(within(form).getByRole("button", { name: "Anahtarı oluştur" }));

    expect(await within(form).findByText("En az bir kapsam seçin")).toBeInTheDocument();
    expect(create).not.toHaveBeenCalled();
  });

  it("binds a logs:ingest key to an HTTP source", async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/api-keys`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(ok({ key: { ...stored, id: 11 }, token: TOKEN }), { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<ApiKeySettings />);

    await user.click(await screen.findByRole("button", { name: "Yeni anahtar" }));
    const form = screen.getByRole("dialog");
    await user.type(within(form).getByLabelText("Ad"), "orders-ingest");
    await user.click(within(form).getByRole("checkbox", { name: /deployments:write/ }));
    await user.click(within(form).getByRole("checkbox", { name: /logs:ingest/ }));
    await user.click(within(form).getByRole("button", { name: "Anahtarı oluştur" }));
    expect(await within(form).findByText("logs:ingest için bir kaynak seçin")).toBeInTheDocument();

    const select = within(form).getByLabelText("Kaynak");
    // Only HTTP sources can receive pushed logs.
    expect(within(select).queryByRole("option", { name: "k8s" })).not.toBeInTheDocument();
    await user.selectOptions(select, "4");
    await user.click(within(form).getByRole("button", { name: "Anahtarı oluştur" }));

    await waitFor(() =>
      expect(body).toEqual({
        name: "orders-ingest",
        scopes: ["logs:ingest"],
        expires_in_days: 365,
        source_id: 4,
      }),
    );
  });

  it("revokes a key only after confirmation", async () => {
    keys = [stored];
    let revoked = 0;
    server.use(
      http.delete(`${API}/api-keys/3`, () => {
        revoked += 1;
        keys = [{ ...stored, active: false, revoked_at: "2026-11-07T10:00:00Z" }];
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<ApiKeySettings />);

    const row = (await screen.findByText("github-actions")).closest("tr")!;
    await user.click(within(row).getByRole("button", { name: "İptal et" }));
    const dialog = await screen.findByRole("alertdialog", {
      name: "“github-actions” iptal edilsin mi?",
    });
    await user.click(within(dialog).getByRole("button", { name: "Anahtarı iptal et" }));

    await waitFor(() => expect(revoked).toBe(1));
    expect(await screen.findByText("İptal edildi")).toBeInTheDocument();
  });
});
