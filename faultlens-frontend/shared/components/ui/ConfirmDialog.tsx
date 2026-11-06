"use client";

import { useRef, useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Modal } from "@/shared/components/ui/Modal";
import { Button } from "@/shared/components/ui/Button";

/* ── Types ─────────────────────────────────────────────────── */

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  /** May return a promise; the dialog shows a busy state and closes when it settles. */
  onConfirm: () => void | Promise<unknown>;
  title: string;
  /** Plain sentence describing the outcome ("This cannot be undone."). */
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Destructive (default) → solid danger button; otherwise the primary button. */
  destructive?: boolean;
}

/* ── Component ─────────────────────────────────────────────── */

/**
 * Confirmation for permanent actions (Instrument "destructive action ladder", step 2):
 * role=alertdialog, the outcome in a plain sentence, a solid danger button and the
 * default focus on "Cancel".
 */
function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel,
  cancelLabel,
  destructive = true,
}: ConfirmDialogProps) {
  const t = useTranslations("common");
  const cancelRef = useRef<HTMLButtonElement>(null);
  const [busy, setBusy] = useState(false);

  const handleConfirm = async () => {
    setBusy(true);
    try {
      await onConfirm();
      onClose();
    } catch {
      // The caller reports the error (toast); keep the dialog open for another try.
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={busy ? () => {} : onClose}
      title={title}
      size="sm"
      role="alertdialog"
      initialFocus={cancelRef}
      showCloseButton={false}
      footer={
        <>
          <Button ref={cancelRef} variant="secondary" onClick={onClose} disabled={busy}>
            {cancelLabel ?? t("cancel")}
          </Button>
          <Button
            variant={destructive ? "danger" : "primary"}
            solid={destructive}
            loading={busy}
            onClick={handleConfirm}
          >
            {confirmLabel ?? (destructive ? t("delete") : t("confirm"))}
          </Button>
        </>
      }
    >
      {description && <p className="small t2">{description}</p>}
    </Modal>
  );
}

/* ── Hook ──────────────────────────────────────────────────── */

type ConfirmRequest = Omit<ConfirmDialogProps, "isOpen" | "onClose">;

/**
 * Imperative helper: `confirm({...})` opens the dialog; render `dialog` once in the component.
 *
 *   const { confirm, dialog } = useConfirm();
 *   confirm({ title, description, onConfirm: () => mutation.mutateAsync(id) });
 */
function useConfirm() {
  const [request, setRequest] = useState<ConfirmRequest | null>(null);
  const dialog = (
    <ConfirmDialog
      isOpen={request !== null}
      onClose={() => setRequest(null)}
      onConfirm={request?.onConfirm ?? (() => {})}
      title={request?.title ?? ""}
      description={request?.description}
      confirmLabel={request?.confirmLabel}
      cancelLabel={request?.cancelLabel}
      destructive={request?.destructive}
    />
  );
  return { confirm: setRequest, dialog };
}

export { ConfirmDialog, useConfirm, type ConfirmDialogProps };
