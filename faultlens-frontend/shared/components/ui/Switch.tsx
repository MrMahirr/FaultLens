"use client";

import { type ButtonHTMLAttributes } from "react";
import { cn } from "@/shared/lib/utils";

interface SwitchProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, "onChange"> {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  /** Accessible name when there is no visible <label htmlFor>. */
  label?: string;
}

/** On/off control (Instrument .switch, role=switch, Space/Enter toggle as a button). */
function Switch({ checked, onCheckedChange, label, className, disabled, ...props }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      aria-disabled={disabled || undefined}
      onClick={() => !disabled && onCheckedChange(!checked)}
      className={cn("switch", className)}
      {...props}
    />
  );
}

export { Switch, type SwitchProps };
