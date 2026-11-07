"use client";

import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type ElementType,
  type ReactNode,
} from "react";
import { useMotionAllowed } from "@/shared/hooks/useMotionAllowed";
import { cn } from "@/shared/lib/utils";

type State = "static" | "wait" | "in";

/**
 * Fades its content up once, the first time it scrolls into view.
 *
 * Content renders visible on the server and without JS; only a block that starts below the
 * fold is hidden, so nothing on screen ever blinks out. No motion setting or reduced motion
 * means no hiding at all. `delay` staggers siblings (ms).
 */
export function Reveal({
  as: Tag = "div",
  delay = 0,
  className,
  style,
  children,
}: {
  as?: ElementType;
  delay?: number;
  className?: string;
  style?: CSSProperties;
  children: ReactNode;
}) {
  const ref = useRef<HTMLElement>(null);
  const motion = useMotionAllowed();
  const [state, setState] = useState<State>("static");

  useEffect(() => {
    const el = ref.current;
    if (!el || !motion || typeof IntersectionObserver === "undefined") return;
    if (el.getBoundingClientRect().top < window.innerHeight) return; // already on screen

    setState("wait");
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          setState("in");
          io.disconnect();
        }
      },
      { rootMargin: "0px 0px -10% 0px" },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [motion]);

  // Motion switched off mid-way: show whatever was still waiting.
  const shown: State = motion ? state : "static";

  return (
    <Tag
      ref={ref}
      data-reveal={shown}
      className={cn(className)}
      style={shown === "in" && delay ? { ...style, transitionDelay: `${delay}ms` } : style}
    >
      {children}
    </Tag>
  );
}
