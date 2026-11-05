import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery } from "@/test/utils";
import { NotificationSettings } from "./NotificationSettings";

const stored = {
  email_enabled: true,
  slack_enabled: false,
  slack_webhook: null,
  webhook_enabled: false,
  webhook_url: null,
  push_enabled: false,
  mobile_enabled: false,
  email: "ops@example.com",
  email_available: true,
};

beforeEach(() => {
  vi.spyOn(toast, "success").mockImplementation(() => "");
  vi.spyOn(toast, "error").mockImplementation(() => "");
});

describe("NotificationSettings (SMTP + webhook)", () => {
  it("shows where alarm e-mails go and sends the test to that address", async () => {
    let tested = 0;
    server.use(
      http.get(`${API}/settings/notifications`, () => HttpResponse.json(ok(stored))),
      http.post(`${API}/settings/notifications/test/email`, () => {
        tested += 1;
        return HttpResponse.json(ok("Test e-postası ops@example.com adresine gönderildi."));
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<NotificationSettings />);

    expect(await screen.findByText("ops@example.com")).toBeInTheDocument();
    expect(screen.queryByText(/Service ID/)).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Test E-postası Gönder" }));
    await waitFor(() => expect(tested).toBe(1));
  });

  it("explains when the server has no SMTP", async () => {
    server.use(
      http.get(`${API}/settings/notifications`, () =>
        HttpResponse.json(ok({ ...stored, email_available: false })),
      ),
    );
    renderWithQuery(<NotificationSettings />);

    expect(await screen.findByText(/SMTP yapılandırılmamış/)).toBeInTheDocument();
  });

  it("saves the webhook URL with the other preferences", async () => {
    let body: unknown;
    server.use(
      http.get(`${API}/settings/notifications`, () =>
        HttpResponse.json(ok({ ...stored, webhook_enabled: true })),
      ),
      http.put(`${API}/settings/notifications`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(ok({ ...stored, ...(body as object) }));
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<NotificationSettings />);

    await user.type(await screen.findByLabelText("Webhook URL"), "https://hooks.example.com/x");
    await user.click(screen.getByRole("button", { name: "Değişiklikleri Kaydet" }));

    await waitFor(() =>
      expect(body).toEqual({
        email_enabled: true,
        slack_enabled: false,
        slack_webhook: null,
        webhook_enabled: true,
        webhook_url: "https://hooks.example.com/x",
        push_enabled: false,
        mobile_enabled: false,
      }),
    );
  });
});
