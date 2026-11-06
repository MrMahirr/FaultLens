import { render, screen, waitFor, within } from "@/test/rtl";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { ConfirmDialog } from "./ConfirmDialog";
import { Modal } from "./Modal";

function Harness() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button onClick={() => setOpen(true)}>Aç</button>
      <Modal isOpen={open} onClose={() => setOpen(false)} title="Başlık">
        <input aria-label="Birinci" />
        <input aria-label="İkinci" />
      </Modal>
    </>
  );
}

describe("Modal", () => {
  it("traps Tab inside the dialog and returns focus to the trigger on Esc", async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const trigger = screen.getByRole("button", { name: "Aç" });

    await user.click(trigger);
    const dialog = screen.getByRole("dialog", { name: "Başlık" });
    const close = within(dialog).getByRole("button", { name: "Kapat" });
    expect(close).toHaveFocus(); // first focusable

    await user.tab();
    expect(within(dialog).getByLabelText("Birinci")).toHaveFocus();
    await user.tab();
    expect(within(dialog).getByLabelText("İkinci")).toHaveFocus();
    await user.tab(); // wraps
    expect(close).toHaveFocus();
    await user.tab({ shift: true }); // wraps backwards
    expect(within(dialog).getByLabelText("İkinci")).toHaveFocus();

    await user.keyboard("{Escape}");
    expect(trigger).toHaveFocus();
    // Removed after the exit animation.
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});

describe("ConfirmDialog", () => {
  it("is an alertdialog with the focus on Cancel; confirm runs the action and closes", async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();
    render(
      <ConfirmDialog
        isOpen
        onClose={onClose}
        onConfirm={onConfirm}
        title="Silinsin mi?"
        description="Geri alınamaz."
      />,
    );

    const dialog = screen.getByRole("alertdialog", { name: "Silinsin mi?" });
    expect(within(dialog).getByRole("button", { name: "Vazgeç" })).toHaveFocus();

    await user.click(within(dialog).getByRole("button", { name: "Sil" }));
    expect(onConfirm).toHaveBeenCalledOnce();
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("stays open when the action fails", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <ConfirmDialog
        isOpen
        onClose={onClose}
        onConfirm={() => Promise.reject(new Error("nope"))}
        title="Silinsin mi?"
      />,
    );

    await user.click(screen.getByRole("button", { name: "Sil" }));
    expect(onClose).not.toHaveBeenCalled();
  });
});
