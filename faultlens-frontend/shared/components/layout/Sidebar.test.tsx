import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen } from "@/test/rtl";
import { useUIStore } from "@/shared/store/ui.store";
import { Sidebar } from "./Sidebar";

vi.mock("next/navigation", () => ({ usePathname: () => "/logs" }));

/** jsdom has no matchMedia; `desktop` decides whether the lg query matches. */
function mockViewport(desktop: boolean) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: query.includes("min-width: 1024px") ? desktop : false,
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
}

describe("Sidebar", () => {
  beforeEach(() => {
    useUIStore.setState({ sidebarOpen: true, mobileNavOpen: false });
  });

  it("is an inert, off-screen drawer on phones until opened", () => {
    mockViewport(false);
    const { container } = render(<Sidebar />);
    const aside = container.querySelector("aside")!;
    expect(aside).toHaveAttribute("inert");
    expect(aside.className).toContain("-translate-x-full");

    act(() => useUIStore.getState().setMobileNavOpen(true));

    expect(aside).not.toHaveAttribute("inert");
    expect(aside).toHaveAttribute("aria-modal", "true");
    expect(screen.getByRole("link", { name: "Loglar" })).toHaveAttribute("aria-current", "page");
  });

  it("closes the drawer with Escape and with its close button", () => {
    mockViewport(false);
    render(<Sidebar />);

    act(() => useUIStore.getState().setMobileNavOpen(true));
    fireEvent.keyDown(document, { key: "Escape" });
    expect(useUIStore.getState().mobileNavOpen).toBe(false);

    act(() => useUIStore.getState().setMobileNavOpen(true));
    fireEvent.click(screen.getByRole("button", { name: "Menüyü kapat" }));
    expect(useUIStore.getState().mobileNavOpen).toBe(false);
  });

  it("stays a focusable rail with the width toggle on desktop", () => {
    mockViewport(true);
    const { container } = render(<Sidebar />);
    expect(container.querySelector("aside")).not.toHaveAttribute("inert");
    expect(screen.getByRole("button", { name: "Daralt" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Menüyü kapat" })).not.toBeInTheDocument();
  });
});
