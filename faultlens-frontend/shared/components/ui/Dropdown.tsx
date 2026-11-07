"use client";

import {
  cloneElement,
  isValidElement,
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactElement,
  type ReactNode,
} from "react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface DropdownItem {
  label: string;
  value: string;
  icon?: ReactNode;
  danger?: boolean;
  disabled?: boolean;
  /** Shows a check mark (menuitemradio semantics). */
  checked?: boolean;
  /** Small trailing hint, e.g. "Sistem · Koyu". */
  hint?: string;
}

interface DropdownProps {
  /** A single button element; it receives aria-haspopup / aria-expanded / onClick. */
  trigger: ReactElement<Record<string, unknown>>;
  items: DropdownItem[];
  onSelect: (value: string) => void;
  align?: "left" | "right";
  label?: string;
  className?: string;
}

/* ── Component (Instrument .menu) ──────────────────────────── */

/**
 * Menu button: Enter/Space/↓ opens and focuses the first item, ↑/↓/Home/End move, Esc or a
 * click outside closes and returns focus to the trigger.
 */
function Dropdown({ trigger, items, onSelect, align = "right", label, className }: DropdownProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const menuId = useId();
  const hasChecks = items.some((i) => i.checked !== undefined);

  const itemEls = () =>
    Array.from(
      menuRef.current?.querySelectorAll<HTMLButtonElement>("[role^=menuitem]") ?? [],
    ).filter((el) => !el.disabled);

  const close = (refocus: boolean) => {
    setOpen(false);
    if (refocus) rootRef.current?.querySelector<HTMLElement>("[aria-haspopup]")?.focus();
  };

  useEffect(() => {
    if (!open) return;
    itemEls()[0]?.focus();
    const onDown = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  const onMenuKeyDown = (e: ReactKeyboardEvent) => {
    const els = itemEls();
    const i = els.indexOf(document.activeElement as HTMLButtonElement);
    if (e.key === "ArrowDown") els[(i + 1) % els.length]?.focus();
    else if (e.key === "ArrowUp") els[(i - 1 + els.length) % els.length]?.focus();
    else if (e.key === "Home") els[0]?.focus();
    else if (e.key === "End") els[els.length - 1]?.focus();
    else if (e.key === "Escape" || e.key === "Tab") {
      if (e.key === "Escape") e.preventDefault();
      close(e.key === "Escape");
      return;
    } else return;
    e.preventDefault();
  };

  const triggerEl = isValidElement(trigger)
    ? cloneElement(trigger, {
        "aria-haspopup": "menu",
        "aria-expanded": open,
        "aria-controls": open ? menuId : undefined,
        onClick: () => setOpen((o) => !o),
        onKeyDown: (e: ReactKeyboardEvent) => {
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setOpen(true);
          }
        },
      })
    : trigger;

  return (
    <div ref={rootRef} className={cn("relative inline-block", className)}>
      {triggerEl}

      {open && (
        <div
          ref={menuRef}
          id={menuId}
          role="menu"
          aria-label={label}
          onKeyDown={onMenuKeyDown}
          className={cn(
            "menu absolute z-50 top-[calc(100%+6px)]",
            align === "right" ? "right-0" : "left-0",
          )}
        >
          {items.map((item) => (
            <button
              key={item.value}
              type="button"
              role={hasChecks ? "menuitemradio" : "menuitem"}
              aria-checked={hasChecks ? !!item.checked : undefined}
              tabIndex={-1}
              disabled={item.disabled}
              onClick={() => {
                onSelect(item.value);
                close(true);
              }}
              className={cn(
                "menu-item",
                item.danger && "text-error",
                item.disabled && "opacity-40 cursor-not-allowed",
              )}
            >
              {item.icon && <span className="shrink-0 inline-flex t3">{item.icon}</span>}
              <span className="truncate">{item.label}</span>
              {item.hint && <span className="k xs t4">{item.hint}</span>}
              {item.checked && (
                <span className="tick" aria-hidden="true">
                  ✓
                </span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export { Dropdown, type DropdownProps, type DropdownItem };

/* ── Select (native <select class="input">) ────────────────── */

interface DropdownSelectProps {
  label?: string;
  value: string;
  options: DropdownItem[];
  onChange: (value: string) => void;
  placeholder?: string;
  error?: string;
  className?: string;
  id?: string;
}

/** Labelled native select: keyboard, screen readers and mobile pickers work out of the box. */
function DropdownSelect({
  label,
  value,
  options,
  onChange,
  placeholder,
  error,
  className,
  id,
}: DropdownSelectProps) {
  const autoId = useId();
  const selectId = id ?? autoId;
  return (
    <div className={cn("field", className)}>
      {label && (
        <label htmlFor={selectId} className="label">
          {label}
        </label>
      )}
      <select
        id={selectId}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${selectId}-error` : undefined}
        className={cn("input", error && "is-error")}
      >
        {placeholder !== undefined && (
          <option value="" disabled={value !== ""}>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value} disabled={option.disabled}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <p id={`${selectId}-error`} className="err" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export { DropdownSelect, type DropdownSelectProps };
