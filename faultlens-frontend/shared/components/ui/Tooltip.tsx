"use client";

import { useId, useState, useRef, useEffect, type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

type TooltipPosition = "top" | "bottom" | "left" | "right";

interface TooltipProps {
  content: ReactNode;
  position?: TooltipPosition;
  delay?: number;
  children: ReactNode;
  className?: string;
}

/* ── Position Styles ───────────────────────────────────────── */

const positionStyles: Record<TooltipPosition, string> = {
  top: "bottom-full left-1/2 -translate-x-1/2 mb-1.5",
  bottom: "top-full left-1/2 -translate-x-1/2 mt-1.5",
  left: "right-full top-1/2 -translate-y-1/2 mr-1.5",
  right: "left-full top-1/2 -translate-y-1/2 ml-1.5",
};

/* ── Component (Instrument .tip-b: inverted chip) ──────────── */

function Tooltip({ content, position = "top", delay = 200, children, className }: TooltipProps) {
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const id = useId();

  const show = () => {
    timeoutRef.current = setTimeout(() => setVisible(true), delay);
  };

  const hide = () => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setVisible(false);
  };

  useEffect(
    () => () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    },
    [],
  );

  return (
    <div
      className="relative inline-flex"
      onMouseEnter={show}
      onMouseLeave={hide}
      onFocus={show}
      onBlur={hide}
      onKeyDown={(e) => e.key === "Escape" && hide()}
      aria-describedby={visible ? id : undefined}
    >
      {children}

      {visible && (
        <div
          id={id}
          role="tooltip"
          className={cn(
            "absolute z-50 px-2 py-1 text-xs rounded-[4px] whitespace-nowrap pointer-events-none",
            "bg-text-primary text-bg-primary animate-fade-in",
            positionStyles[position],
            className,
          )}
        >
          {content}
        </div>
      )}
    </div>
  );
}

export { Tooltip, type TooltipProps, type TooltipPosition };
