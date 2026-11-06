"use client";

import { useRef, type KeyboardEvent, type ReactNode } from "react";
import { cn } from "@/shared/lib/utils";

interface TabItem<T extends string> {
  value: T;
  label: ReactNode;
  icon?: ReactNode;
  /** Small mono counter next to the label (Instrument `.tab .n`). */
  count?: number;
}

interface TabsProps<T extends string> {
  items: TabItem<T>[];
  value: T;
  onChange: (value: T) => void;
  label: string;
  /** Tab ids are `${idPrefix}-tab-${value}`; panels use `${idPrefix}-panel-${value}`. */
  idPrefix: string;
  className?: string;
}

/**
 * WAI-ARIA tablist (Instrument .tabs): ←/→/Home/End move and activate, only the selected tab
 * is in the Tab order.
 */
function Tabs<T extends string>({
  items,
  value,
  onChange,
  label,
  idPrefix,
  className,
}: TabsProps<T>) {
  const listRef = useRef<HTMLDivElement>(null);

  const onKeyDown = (e: KeyboardEvent) => {
    const index = items.findIndex((item) => item.value === value);
    let next = index;
    if (e.key === "ArrowRight") next = (index + 1) % items.length;
    else if (e.key === "ArrowLeft") next = (index - 1 + items.length) % items.length;
    else if (e.key === "Home") next = 0;
    else if (e.key === "End") next = items.length - 1;
    else return;
    e.preventDefault();
    onChange(items[next].value);
    listRef.current?.querySelectorAll<HTMLButtonElement>("[role=tab]")[next]?.focus();
  };

  return (
    <div
      ref={listRef}
      role="tablist"
      aria-label={label}
      className={cn("tabs", className)}
      onKeyDown={onKeyDown}
    >
      {items.map((item) => {
        const selected = item.value === value;
        return (
          <button
            key={item.value}
            type="button"
            role="tab"
            id={`${idPrefix}-tab-${item.value}`}
            aria-selected={selected}
            aria-controls={`${idPrefix}-panel-${item.value}`}
            tabIndex={selected ? 0 : -1}
            className="tab"
            onClick={() => onChange(item.value)}
          >
            {item.icon && (
              <span className="inline-flex" aria-hidden="true">
                {item.icon}
              </span>
            )}
            {item.label}
            {item.count !== undefined && <span className="n">{item.count}</span>}
          </button>
        );
      })}
    </div>
  );
}

export { Tabs, type TabItem, type TabsProps };
