import { screen, waitFor, within } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import toast from "react-hot-toast";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import type { LogSourceDto } from "../types/source.types";
import { SourceCard } from "./SourceCard";

const source = {
  id: 7,
  name: "orders-api",
  type: "DOCKER",
  config: "{}",
  enabled: true,
} as unknown as LogSourceDto;

beforeEach(() => {
  vi.spyOn(toast, "success").mockImplementation(() => "");
  vi.spyOn(toast, "error").mockImplementation(() => "");
});

describe("SourceCard roles (P07)", () => {
  it("a VIEWER sees neither the action menu nor the test button", () => {
    signInAs("VIEWER");
    renderWithQuery(<SourceCard source={source} index={0} />);

    expect(screen.getByText("orders-api")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Kaynak işlemleri" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /test/i })).not.toBeInTheDocument();
  });

  it("an EDITOR can toggle but not delete", async () => {
    signInAs("EDITOR");
    const user = userEvent.setup();
    renderWithQuery(<SourceCard source={source} index={0} />);

    await user.click(screen.getByRole("button", { name: "Kaynak işlemleri" }));
    expect(screen.getByText("Devre Dışı Bırak")).toBeInTheDocument();
    expect(screen.queryByText("Sil")).not.toBeInTheDocument();
  });

  it("an ADMIN deletes after confirming", async () => {
    signInAs("ADMIN");
    let deleted = 0;
    server.use(
      http.delete(`${API}/sources/7`, () => {
        deleted += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderWithQuery(<SourceCard source={source} index={0} />);

    await user.click(screen.getByRole("button", { name: "Kaynak işlemleri" }));
    await user.click(screen.getByRole("menuitem", { name: "Sil" }));
    const dialog = await screen.findByRole("alertdialog");
    await user.click(within(dialog).getByRole("button", { name: "Kaynağı sil" }));
    await waitFor(() => expect(deleted).toBe(1));
  });

  it("shows the collector's last error while the source is failing", () => {
    signInAs("VIEWER");
    renderWithQuery(
      <SourceCard
        source={
          {
            ...source,
            health: "ERROR",
            lastError: "Dosya izin verilen dizinlerin dışında (COLLECTOR_FILE_ALLOWED_DIRS)",
            lastErrorAt: new Date().toISOString(),
            collectorInstance: "api-1-ab12cd34",
          } as LogSourceDto
        }
        index={0}
      />,
    );

    expect(screen.getByTestId("source-last-error")).toHaveTextContent(
      "izin verilen dizinlerin dışında",
    );
    expect(screen.getByText("api-1-ab12cd34")).toBeInTheDocument();
  });
});
