import { screen } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { LogContext } from "./LogContext";

const entry = (id: number, message: string) => ({
  id,
  source_id: 1,
  group_id: null,
  severity: "INFO",
  message,
  pod_name: "api-7d9",
  timestamp: `2026-11-09T10:00:${String(id % 60).padStart(2, "0")}Z`,
});

describe("LogContext", () => {
  const requests: URLSearchParams[] = [];

  beforeEach(() => {
    signInAs("VIEWER");
    requests.length = 0;
    // jsdom has no layout; the component scrolls the anchor into view.
    Element.prototype.scrollIntoView = vi.fn();
    server.use(
      http.get(`${API}/logs/:id/context`, ({ params, request }) => {
        const query = new URL(request.url).searchParams;
        requests.push(query);
        if (params.id === "10") {
          return HttpResponse.json(
            ok({
              anchor_id: 10,
              entries: [entry(9, "before"), entry(10, "the entry"), entry(11, "after")],
              has_more_before: true,
              has_more_after: false,
              window_minutes: 60,
            }),
          );
        }
        // "Older lines": the context of entry 9 with nothing after it (9 itself included).
        return HttpResponse.json(
          ok({
            anchor_id: 9,
            entries: [entry(7, "older 1"), entry(8, "older 2"), entry(9, "before")],
            has_more_before: false,
            has_more_after: false,
            window_minutes: 60,
          }),
        );
      }),
    );
  });

  it("highlights the entry and loads older lines without repeating the edge", async () => {
    renderWithQuery(<LogContext log={{ id: 10, podName: "api-7d9" }} />);

    expect(await screen.findByText("the entry")).toBeInTheDocument();
    expect(requests[0].get("before")).toBe("50");
    expect(requests[0].get("after")).toBe("50");
    expect(screen.getByText("the entry").closest("button")).toHaveAttribute("aria-current", "true");
    expect(screen.getByText(/api-7d9/)).toBeInTheDocument();
    // Nothing newer: no "newer lines" button.
    expect(screen.queryByRole("button", { name: "Daha yeni satırlar" })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Daha eski satırlar" }));

    expect(await screen.findByText("older 1")).toBeInTheDocument();
    expect(requests[1].get("before")).toBe("50");
    expect(requests[1].get("after")).toBe("0");
    expect(screen.getAllByText("before")).toHaveLength(1);
    const order = screen.getAllByRole("listitem").map((item) => item.textContent ?? "");
    expect(order.map((text) => text.replace(/^.*INFO/, ""))).toEqual([
      "older 1",
      "older 2",
      "before",
      "the entry",
      "after",
    ]);
    expect(screen.queryByRole("button", { name: "Daha eski satırlar" })).not.toBeInTheDocument();
  });
});
