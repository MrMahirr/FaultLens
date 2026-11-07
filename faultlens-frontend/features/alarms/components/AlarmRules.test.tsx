import { screen, waitFor, within } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { AlarmRulesList } from "./AlarmRulesList";
import { CreateRuleModal } from "./CreateRuleModal";

const storedRule = {
  id: 4,
  name: "Hata patlaması",
  type: "THRESHOLD",
  severity: "ERROR",
  window_sec: 300,
  threshold: 50,
  pattern: null,
  source_ids: [],
  channels: ["IN_APP", "SLACK"],
  cooldown_sec: 900,
  enabled: true,
  created_by: "admin",
  created_at: "2026-11-05T10:00:00Z",
  updated_at: "2026-11-05T10:00:00Z",
};

beforeEach(() => {
  signInAs("EDITOR");
  vi.spyOn(toast, "success").mockImplementation(() => "");
  vi.spyOn(toast, "error").mockImplementation(() => "");
  server.use(
    http.get(`${API}/sources`, () =>
      HttpResponse.json(
        ok([{ id: 7, name: "orders-api", type: "DOCKER", config: "{}", enabled: true }]),
      ),
    ),
  );
});

describe("CreateRuleModal", () => {
  it("posts a threshold rule in the backend's shape", async () => {
    let body: unknown;
    server.use(
      http.post(`${API}/alarms/rules`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(ok(storedRule), { status: 201 });
      }),
    );
    const onClose = vi.fn();
    const user = userEvent.setup();
    renderWithQuery(<CreateRuleModal isOpen onClose={onClose} />);

    await user.type(screen.getByLabelText("Kural Adı"), "Hata patlaması");
    const windowInput = screen.getByLabelText("Pencere (dakika)");
    await user.clear(windowInput);
    await user.type(windowInput, "10");
    await user.click(await screen.findByLabelText("orders-api"));
    await user.click(screen.getByLabelText("Slack"));
    expect(screen.getByTestId("rule-preview")).toHaveTextContent(
      "10 dk içinde ≥ 50 adet ERROR+ log",
    );
    await user.click(screen.getByRole("button", { name: /kuralı ekle/i }));

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(body).toEqual({
      name: "Hata patlaması",
      type: "THRESHOLD",
      severity: "ERROR",
      window_sec: 600,
      threshold: 50,
      source_ids: [7],
      channels: ["IN_APP", "SLACK"],
      cooldown_sec: 900,
    });
  });

  it("shows pattern validation before sending anything", async () => {
    const posted = vi.fn();
    server.use(http.post(`${API}/alarms/rules`, posted));
    const user = userEvent.setup();
    renderWithQuery(<CreateRuleModal isOpen onClose={vi.fn()} />);

    await user.type(screen.getByLabelText("Kural Adı"), "Bağlantı reddi");
    await user.selectOptions(screen.getByLabelText("Kural Tipi"), "PATTERN");
    await user.type(screen.getByLabelText("Desen (düzenli ifade)"), "(unclosed");
    await user.click(screen.getByRole("button", { name: /kuralı ekle/i }));

    expect(await screen.findByText("Geçersiz düzenli ifade")).toBeInTheDocument();
    expect(posted).not.toHaveBeenCalled();
  });
});

describe("AlarmRulesList", () => {
  it("lists stored rules with a readable condition and toggles them", async () => {
    server.use(http.get(`${API}/alarms/rules`, () => HttpResponse.json(ok([storedRule]))));
    let disabled = 0;
    server.use(
      http.post(`${API}/alarms/rules/4/disable`, () => {
        disabled += 1;
        return HttpResponse.json(ok({ ...storedRule, enabled: false }));
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<AlarmRulesList />);

    const row = (await screen.findByText("Hata patlaması")).closest("tr")!;
    expect(within(row).getByText("5 dk içinde ≥ 50 adet ERROR+ log")).toBeInTheDocument();
    expect(within(row).getByText("Uygulama içi, Slack")).toBeInTheDocument();
    expect(within(row).getByText("Aktif")).toBeInTheDocument();

    await user.click(within(row).getByRole("button", { name: "Pasife Al" }));
    await waitFor(() => expect(disabled).toBe(1));
  });

  it("deletes only after confirmation", async () => {
    server.use(http.get(`${API}/alarms/rules`, () => HttpResponse.json(ok([storedRule]))));
    let deleted = 0;
    server.use(
      http.delete(`${API}/alarms/rules/4`, () => {
        deleted += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<AlarmRulesList />);
    const row = (await screen.findByText("Hata patlaması")).closest("tr")!;

    await user.click(within(row).getByRole("button", { name: "Sil" }));
    const dialog = await screen.findByRole("alertdialog", { name: "Kural silinsin mi?" });
    // Destructive confirmations focus "Vazgeç" first.
    expect(within(dialog).getByRole("button", { name: "Vazgeç" })).toHaveFocus();
    await user.click(within(dialog).getByRole("button", { name: "Vazgeç" }));
    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
    expect(deleted).toBe(0);

    await user.click(within(row).getByRole("button", { name: "Sil" }));
    const again = await screen.findByRole("alertdialog");
    await user.click(within(again).getByRole("button", { name: "Sil" }));
    await waitFor(() => expect(deleted).toBe(1));
  });
});
