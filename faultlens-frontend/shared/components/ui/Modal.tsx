"use client";

import { useEffect, useId, useRef, type ReactNode, type RefObject } from "react";
import { createPortal } from "react-dom";
import { motion, AnimatePresence } from "framer-motion";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";
import { useFocusTrap } from "@/shared/hooks/useFocusTrap";
import { useIsClient } from "@/shared/hooks/useIsClient";

/* ── Types ─────────────────────────────────────────────────── */

type ModalSize = "sm" | "md" | "lg" | "xl" | "full";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  size?: ModalSize;
  showCloseButton?: boolean;
  closeOnBackdrop?: boolean;
  closeOnEsc?: boolean;
  /** "alertdialog" for confirmations of destructive actions. */
  role?: "dialog" | "alertdialog";
  /** Element focused on open (e.g. the "Cancel" button of a destructive confirmation). */
  initialFocus?: RefObject<HTMLElement | null>;
  children: ReactNode;
  footer?: ReactNode;
  className?: string;
}

/* ── Style Maps ────────────────────────────────────────────── */

const sizeStyles: Record<ModalSize, string> = {
  sm: "max-w-sm",
  md: "max-w-[520px]",
  lg: "max-w-xl",
  xl: "max-w-2xl",
  full: "max-w-[90vw]",
};

/* ── Component (Instrument .scrim / .modal) ────────────────── */

function Modal({
  isOpen,
  onClose,
  title,
  description,
  size = "md",
  showCloseButton = true,
  closeOnBackdrop = true,
  closeOnEsc = true,
  role = "dialog",
  initialFocus,
  children,
  footer,
  className,
}: ModalProps) {
  const t = useTranslations("common");
  const isClient = useIsClient();
  const modalRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useFocusTrap(modalRef, isOpen, initialFocus);

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (closeOnEsc && e.key === "Escape") {
        e.stopPropagation();
        onClose();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen, closeOnEsc, onClose]);

  if (!isClient) return null;

  return createPortal(
    <AnimatePresence>
      {isOpen && (
        <motion.div
          // framer-motion animates enter and exit; the CSS keyframes of .scrim/.modal would
          // run a second, enter-only animation on top, so they are switched off here.
          className="scrim center animate-none"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.15 }}
          onMouseDown={(e) => {
            if (closeOnBackdrop && e.target === e.currentTarget) onClose();
          }}
        >
          <motion.div
            ref={modalRef}
            role={role}
            aria-modal="true"
            aria-labelledby={title ? titleId : undefined}
            aria-describedby={description ? descriptionId : undefined}
            tabIndex={-1}
            initial={{ opacity: 0, y: 4, scale: 0.985 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.985 }}
            transition={{ duration: 0.2, ease: [0.2, 0, 0, 1] }}
            className={cn(
              "modal flex flex-col outline-none animate-none",
              sizeStyles[size],
              className,
            )}
          >
            {(title || showCloseButton) && (
              <div className="modal-h">
                <div className="min-w-0">
                  {title && (
                    <h2 id={titleId} className="h2">
                      {title}
                    </h2>
                  )}
                  {description && (
                    <p id={descriptionId} className="small t3 mt-1">
                      {description}
                    </p>
                  )}
                </div>
                {showCloseButton && (
                  <button
                    type="button"
                    onClick={onClose}
                    className="btn btn-ghost btn-sm btn-icon shrink-0"
                    aria-label={t("close")}
                  >
                    <X size={16} aria-hidden="true" />
                  </button>
                )}
              </div>
            )}

            <div className="modal-b overflow-y-auto">{children}</div>

            {footer && <div className="modal-f">{footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body,
  );
}

export { Modal, type ModalProps, type ModalSize };
