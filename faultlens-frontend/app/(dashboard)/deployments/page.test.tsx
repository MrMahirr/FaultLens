import { screen } from "@/test/rtl";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import DeploymentsPage from "./page";

/** The URL the page is opened with; the page keeps its filters there. */
let search = "";
const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
  usePathname: () => "/deployments",
  useSearchParams: () => new URLSearchParams(search),
}));

const deployment = {
  id: 9,
  service_name: "payments-api",
  version: "v2.1.0",
  environment: "PRODUCTION",
  deployed_at: "2026-11-05T10:00:00Z",
  deployed_by: "ci",
  status: "SUCCESS",
  notes: "Pool size restored",
};

describe("DeploymentsPage", () => {
  let requested: URLSearchParams | undefined;

  beforeEach(() => {
    signInAs("VIEWER");
    requested = undefined;
    replace.mockClear();
    server.use(
      http.get(`${API}/deployments`, ({ request }) => {
        requested = new URL(request.url).searchParams;
        return HttpResponse.json(
          ok({ content: [deployment], page: 1, size: 20, total_elements: 45, total_pages: 3 }),
        );
      }),
    );
  });

  it("filters and pages on the server, from the URL", async () => {
    search = "q=pay&env=PRODUCTION&page=2";
    renderWithQuery(<DeploymentsPage />);

    expect(await screen.findByText("payments-api")).toBeInTheDocument();
    expect(requested?.get("q")).toBe("pay");
    expect(requested?.get("environment")).toBe("PRODUCTION");
    expect(requested?.get("page")).toBe("1"); // the API is zero-based
    expect(requested?.get("size")).toBe("20");

    expect(screen.getByText("45 deployment")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sayfa 2" })).toHaveAttribute("aria-current", "page");
    // The deployment's notes (the API field) are shown.
    expect(screen.getByText("Pool size restored")).toBeInTheDocument();
  });

  it("sends no filter when the URL has none", async () => {
    search = "";
    renderWithQuery(<DeploymentsPage />);

    await screen.findByText("payments-api");
    expect(requested?.has("q")).toBe(false);
    expect(requested?.has("environment")).toBe(false);
    expect(requested?.get("page")).toBe("0");
  });
});
