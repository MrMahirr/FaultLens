import { screen, waitFor } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { IngestSetup } from "./IngestSetup";

const TOKEN = "flk_Zz99Yy88_0123456789abcdefghijABCDEFGHIJkl";

beforeEach(() => {
  vi.spyOn(toast, "error").mockImplementation(() => "");
});

describe("IngestSetup", () => {
  it("lets an admin create a key bound to the source and shows it in the snippets", async () => {
    signInAs("ADMIN");
    let body: unknown;
    server.use(
      http.post(`${API}/api-keys`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          ok({ key: { id: 1, name: "orders-http-ingest", scopes: ["logs:ingest"] }, token: TOKEN }),
          { status: 201 },
        );
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<IngestSetup sourceId={7} sourceName="orders-http" />);

    // Before a key exists the snippets reference the environment variable.
    expect(screen.getByLabelText("JSON (curl)")).toHaveTextContent("$FAULTLENS_API_KEY");
    expect(screen.getByLabelText("OpenTelemetry Collector")).toHaveTextContent(
      "otlphttp/faultlens",
    );

    await user.click(screen.getByRole("button", { name: "Ingest anahtarı oluştur" }));

    await waitFor(() =>
      expect(body).toEqual({
        name: "orders-http-ingest",
        scopes: ["logs:ingest"],
        source_id: 7,
        expires_in_days: 365,
      }),
    );
    expect(await screen.findByLabelText("API key")).toHaveTextContent(TOKEN);
    expect(screen.getByLabelText("JSON (curl)")).toHaveTextContent(TOKEN);
  });

  it("tells editors that an admin has to create the key", () => {
    signInAs("EDITOR");
    renderWithQuery(<IngestSetup sourceId={7} sourceName="orders-http" />);

    expect(
      screen.queryByRole("button", { name: "Ingest anahtarı oluştur" }),
    ).not.toBeInTheDocument();
    expect(screen.getByText(/Anahtarı bir yönetici oluşturmalı/)).toBeInTheDocument();
  });
});
