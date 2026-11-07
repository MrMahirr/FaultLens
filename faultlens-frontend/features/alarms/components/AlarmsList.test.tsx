import { screen, waitFor, within } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { AlarmsList } from "./AlarmsList";

const alarm = {
  id: 7,
  rule_id: "AI_ANALYSIS",
  rule_name: "Yapay Zeka Kritik Hata Tespiti",
  source_id: null,
  severity: "CRITICAL",
  status: "ACTIVE",
  message: "Hata Analizi (Grup ID: 1)",
  triggered_at: "2026-11-05T10:00:00Z",
  resolved_at: null,
};

describe("AlarmsList delete", () => {
  let deleted: number;

  beforeEach(() => {
    signInAs("ADMIN");
    deleted = 0;
    vi.spyOn(toast, "success").mockImplementation(() => "");
    server.use(
      http.get(`${API}/alarms`, () =>
        HttpResponse.json(
          ok({ content: [alarm], page: 0, size: 20, total_elements: 1, total_pages: 1 }),
        ),
      ),
      http.delete(`${API}/alarms/7`, () => {
        deleted += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
  });

  it("asks before deleting and does nothing when cancelled", async () => {
    renderWithQuery(<AlarmsList />);

    await userEvent.click(await screen.findByRole("button", { name: "Sil" }));
    const dialog = await screen.findByRole("alertdialog", { name: "Alarm silinsin mi?" });
    await userEvent.click(within(dialog).getByRole("button", { name: "Vazgeç" }));

    await waitFor(() => expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument());
    expect(deleted).toBe(0);
  });

  it("deletes once the user confirms", async () => {
    renderWithQuery(<AlarmsList />);

    await userEvent.click(await screen.findByRole("button", { name: "Sil" }));
    const dialog = await screen.findByRole("alertdialog");
    await userEvent.click(within(dialog).getByRole("button", { name: "Sil" }));

    await waitFor(() => expect(deleted).toBe(1));
  });

  it("labels the icon-only action buttons for screen readers", async () => {
    renderWithQuery(<AlarmsList />);

    expect(await screen.findByRole("button", { name: "Üstlen" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Çöz" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sil" })).toBeInTheDocument();
  });
});
