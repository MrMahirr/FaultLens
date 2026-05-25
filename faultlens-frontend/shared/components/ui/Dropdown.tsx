"use client";

import { useState, useRef, useEffect, type ReactNode } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown } from "lucide-react";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface DropdownItem {
  label: string;
  value: string;
  icon?: ReactNode;
  danger?: boolean;
  disabled?: boolean;
}

interface DropdownProps {
  trigger: ReactNode;
  items: DropdownItem[];
  onSelect: (value: string) => void;
  align?: "left" | "right";
  className?: string;
}

interface DropdownSelectProps {
  label?: string;
  value: string;
  options: DropdownItem[];
  onChange: (value: string) => void;
  placeholder?: string;
  error?: string;
  className?: string;
}

/* ── Dropdown Menu Component ───────────────────────────────── */

function Dropdown({
  trigger,
  items,
  onSelect,
  align = "right",
  className,
}: DropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (item: DropdownItem) => {
    if (item.disabled) return;
    onSelect(item.value);
    setIsOpen(false);
  };

  return (
    <div ref={dropdownRef} className={cn("relative inline-block", className)}>
      <div onClick={() => setIsOpen(!isOpen)} className="cursor-pointer">
        {trigger}
      </div>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.96 }}
            transition={{ duration: 0.15 }}
            className={cn(
              "absolute z-50 mt-1.5 min-w-[160px]",
              "bg-bg-secondary border border-border-default rounded-xl shadow-lg",
              "py-1 overflow-hidden",
              align === "right" ? "right-0" : "left-0"
            )}
          >
            {items.map((item) => (
              <button
                key={item.value}
                onClick={() => handleSelect(item)}
                disabled={item.disabled}
                className={cn(
                  "w-full flex items-center gap-2 px-3 py-2 text-sm text-left",
                  "transition-colors duration-150",
                  item.danger
                    ? "text-error hover:bg-error/10"
                    : "text-text-primary hover:bg-bg-tertiary",
                  item.disabled && "opacity-40 cursor-not-allowed"
                )}
              >
                {item.icon && (
                  <span className="shrink-0 text-text-muted">{item.icon}</span>
                )}
                {item.label}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ── Dropdown Select Component ─────────────────────────────── */

function DropdownSelect({
  label,
  value,
  options,
  onChange,
  placeholder = "Seçiniz...",
  error,
  className,
}: DropdownSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const selectRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        selectRef.current &&
        !selectRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedOption = options.find((opt) => opt.value === value);

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      {label && (
        <label className="text-sm font-medium text-text-secondary">
          {label}
        </label>
      )}

      <div ref={selectRef} className="relative">
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            "w-full flex items-center justify-between",
            "bg-bg-tertiary border border-border-default rounded-lg",
            "px-3 py-2 text-sm transition-all duration-200",
            "focus:outline-none focus:border-border-active focus:ring-1 focus:ring-accent/30",
            isOpen && "border-border-active ring-1 ring-accent/30",
            error && "border-error",
            selectedOption ? "text-text-primary" : "text-text-muted"
          )}
        >
          <span className="truncate">
            {selectedOption ? selectedOption.label : placeholder}
          </span>
          <ChevronDown
            size={16}
            className={cn(
              "shrink-0 text-text-muted transition-transform duration-200",
              isOpen && "rotate-180"
            )}
          />
        </button>

        <AnimatePresence>
          {isOpen && (
            <motion.div
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -4 }}
              transition={{ duration: 0.12 }}
              className="absolute z-50 w-full mt-1 bg-bg-secondary border border-border-default rounded-xl shadow-lg py-1 max-h-48 overflow-y-auto"
            >
              {options.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => {
                    onChange(option.value);
                    setIsOpen(false);
                  }}
                  disabled={option.disabled}
                  className={cn(
                    "w-full flex items-center gap-2 px-3 py-2 text-sm text-left",
                    "transition-colors duration-150",
                    option.value === value
                      ? "bg-accent/10 text-accent"
                      : "text-text-primary hover:bg-bg-tertiary",
                    option.disabled && "opacity-40 cursor-not-allowed"
                  )}
                >
                  {option.icon && <span className="shrink-0">{option.icon}</span>}
                  {option.label}
                </button>
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {error && (
        <p className="text-xs text-error flex items-center gap-1">
          <span className="inline-block w-1 h-1 rounded-full bg-error" />
          {error}
        </p>
      )}
    </div>
  );
}

export {
  Dropdown,
  DropdownSelect,
  type DropdownProps,
  type DropdownSelectProps,
  type DropdownItem,
};
