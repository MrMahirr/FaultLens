import { screen } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { API, ok, server } from "@/test/msw/server";
import { renderWithQuery, signInAs } from "@/test/utils";
import { GroupComments } from "./GroupComments";
import { MergedBanner } from "./GroupMerge";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  usePathname: () => "/logs/groups/7",
  useSearchParams: () => new URLSearchParams(),
}));

const comment = (id: number, author: string, body: string) => ({
  id,
  group_id: 7,
  author,
  body,
  created_at: "2026-11-08T10:00:00Z",
  edited_at: null,
});

describe("GroupComments", () => {
  let posted: unknown;

  beforeEach(() => {
    posted = undefined;
    server.use(
      http.get(`${API}/logs/groups/7/comments`, () =>
        HttpResponse.json(
          ok([comment(1, "editor", "<b>bold?</b>\nsecond line"), comment(2, "someone", "theirs")]),
        ),
      ),
      http.post(`${API}/logs/groups/7/comments`, async ({ request }) => {
        posted = await request.json();
        return HttpResponse.json(ok(comment(3, "editor", "new")), { status: 201 });
      }),
    );
  });

  it("shows bodies as text, never as HTML", async () => {
    signInAs("VIEWER");
    const { container } = renderWithQuery(<GroupComments groupId={7} />);

    expect(await screen.findByText(/<b>bold\?<\/b>/)).toBeInTheDocument();
    expect(container.querySelector("b")).toBeNull();
    // Viewers read but do not write.
    expect(screen.queryByRole("button", { name: "Not ekle" })).not.toBeInTheDocument();
  });

  it("lets editors add notes and edit only their own", async () => {
    signInAs("EDITOR", "editor");
    renderWithQuery(<GroupComments groupId={7} />);

    await screen.findByText("theirs");
    // One "Düzenle" — on the editor's own note, not on someone else's.
    expect(screen.getAllByRole("button", { name: "Düzenle" })).toHaveLength(1);

    await userEvent.type(screen.getByLabelText("Yeni not"), "  pool size raised  ");
    await userEvent.click(screen.getByRole("button", { name: "Not ekle" }));
    await vi.waitFor(() => expect(posted).toEqual({ body: "pool size raised" }));
  });
});

describe("MergedBanner", () => {
  it("links to the target and offers unmerge to admins only", () => {
    signInAs("EDITOR");
    const { unmount } = renderWithQuery(<MergedBanner group={{ id: 7, mergedIntoId: 3 }} />);
    expect(screen.getByRole("link", { name: "Hedef grubu aç" })).toHaveAttribute(
      "href",
      "/logs/groups/3",
    );
    expect(screen.queryByRole("button", { name: "Ayır" })).not.toBeInTheDocument();
    unmount();

    signInAs("ADMIN");
    renderWithQuery(<MergedBanner group={{ id: 7, mergedIntoId: 3 }} />);
    expect(screen.getByRole("button", { name: "Ayır" })).toBeInTheDocument();
  });

  it("renders nothing for a group that is not merged", () => {
    signInAs("ADMIN");
    const { container } = renderWithQuery(<MergedBanner group={{ id: 7, mergedIntoId: null }} />);
    expect(container).toBeEmptyDOMElement();
  });
});
