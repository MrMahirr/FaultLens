import { screen, within } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import LogGroupsPage from "./page";
import { undoUpdate } from "@/features/logs/components/GroupActions";
import { GroupStatus } from "@/features/logs/types/log.types";

let search = "";
const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
  usePathname: () => "/logs/groups",
  useSearchParams: () => new URLSearchParams(search),
}));

const group = {
  id: 7,
  fingerprint: "abc",
  first_message: "java.lang.IllegalStateException: pool exhausted",
  severity: "ERROR",
  count: 42,
  first_seen_at: "2026-11-01T10:00:00Z",
  last_seen_at: "2026-11-08T10:00:00Z",
  source_id: 1,
  status: "UNRESOLVED",
  resolved_at: null,
  resolved_by: null,
  ignore_until: null,
  regressed_at: "2026-11-08T10:00:00Z",
  regression_count: 2,
};

describe("LogGroupsPage", () => {
  let requested: URLSearchParams | undefined;
  let patched: { id: string; body: unknown } | undefined;

  beforeEach(() => {
    requested = undefined;
    patched = undefined;
    server.use(
      http.get(`${API}/logs/groups`, ({ request }) => {
        requested = new URL(request.url).searchParams;
        return HttpResponse.json(
          ok({ content: [group], page: 0, size: 25, total_elements: 1, total_pages: 1 }),
        );
      }),
      http.patch(`${API}/logs/groups/:id`, async ({ params, request }) => {
        patched = { id: String(params.id), body: await request.json() };
        return HttpResponse.json(ok({ ...group, status: "RESOLVED", resolved_by: "admin" }));
      }),
    );
  });

  it("lists unresolved groups by default and marks regressions", async () => {
    signInAs("VIEWER");
    search = "";
    renderWithQuery(<LogGroupsPage />);

    expect(await screen.findByText(group.first_message)).toBeInTheDocument();
    expect(requested?.get("status")).toBe("UNRESOLVED");
    expect(screen.getByText("2 regresyon")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Çözülmemiş" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    // Viewers see no actions; the API would refuse them anyway.
    expect(screen.queryByRole("button", { name: "Çöz" })).not.toBeInTheDocument();
  });

  it("reads the status tab from the URL; 'all' asks for every status", async () => {
    signInAs("VIEWER");
    search = "status=all";
    renderWithQuery(<LogGroupsPage />);

    await screen.findByText(group.first_message);
    expect(requested?.get("status")).toBe("UNRESOLVED,RESOLVED,IGNORED");
  });

  it("resolves a group for editors", async () => {
    signInAs("EDITOR");
    search = "";
    renderWithQuery(<LogGroupsPage />);

    const row = (await screen.findByText(group.first_message)).closest("tr")!;
    await userEvent.click(within(row).getByRole("button", { name: "Çöz" }));

    await vi.waitFor(() => expect(patched).toEqual({ id: "7", body: { status: "RESOLVED" } }));
  });
});

describe("undoUpdate", () => {
  const now = Date.parse("2026-11-08T12:00:00Z");

  it("restores the previous status", () => {
    expect(undoUpdate({ status: GroupStatus.UNRESOLVED, ignoreUntil: null }, now)).toEqual({
      status: GroupStatus.UNRESOLVED,
    });
  });

  it("keeps a future ignore period and drops an expired one", () => {
    expect(
      undoUpdate({ status: GroupStatus.IGNORED, ignoreUntil: "2026-11-09T00:00:00Z" }, now),
    ).toEqual({ status: GroupStatus.IGNORED, ignoreUntil: "2026-11-09T00:00:00Z" });
    expect(
      undoUpdate({ status: GroupStatus.IGNORED, ignoreUntil: "2026-11-08T00:00:00Z" }, now),
    ).toEqual({ status: GroupStatus.IGNORED });
  });
});
