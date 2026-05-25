"use client";

import { useState, useRef, useEffect, type ReactNode } from "react";
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
  top: "bottom-full left-1/2 -translate-x-1/2 mb-2",
  bottom: "top-full left-1/2 -translate-x-1/2 mt-2",
  left: "right-full top-1/2 -translate-y-1/2 mr-2",
  right: "left-full top-1/2 -translate-y-1/2 ml-2",
};

const arrowStyles: Record<TooltipPosition, string> = {
  top: "top-full left-1/2 -translate-x-1/2 border-t-bg-tertiary border-x-transparent border-b-transparent",
  bottom: "bottom-full left-1/2 -translate-x-1/2 border-b-bg-tertiary border-x-transparent border-t-transparent",
  left: "left-full top-1/2 -translate-y-1/2 border-l-bg-tertiary border-y-transparent border-r-transparent",
  right: "right-full top-1/2 -translate-y-1/2 border-r-bg-tertiary border-y-transparent border-l-transparent",
};

/* ── Component ─────────────────────────────────────────────── */

function Tooltip({
  content,
  position = "top",
  delay = 200,
  children,
  className,
}: TooltipProps) {
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout>>();

  const showTooltip = () => {
    timeoutRef.current = setTimeout(() => setVisible(true), delay);
  };

  const hideTooltip = () => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setVisible(false);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  return (
    <div
      className="relative inline-flex"
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocus={showTooltip}
      onBlur={hideTooltip}
    >
      {children}

      {visible && (
        <div
          role="tooltip"
          className={cn(
            "absolute z-50 px-2.5 py-1.5 text-xs font-medium",
            "bg-bg-tertiary text-text-primary rounded-lg",
            "border border-border-default shadow-md",
            "whitespace-nowrap pointer-events-none",
            "animate-fade-in",
            positionStyles[position],
            className
          )}
        >
          {content}
          <span
            className={cn(
              "absolute w-0 h-0 border-4",
              arrowStyles[position]
            )}
          />
        </div>
      )}
    </div>
  );
}

export { Tooltip, type TooltipProps, type TooltipPosition };
