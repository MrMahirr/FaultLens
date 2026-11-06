"use client";

import { forwardRef, useState, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { Eye, EyeOff } from "lucide-react";
import { Input, type InputProps } from "@/shared/components/ui/Input";

/**
 * Password field with show/hide toggle and a Caps Lock warning (Instrument identity patterns).
 */
const PasswordInput = forwardRef<HTMLInputElement, Omit<InputProps, "type">>(
  ({ hint, onKeyUp, onKeyDown, ...props }, ref) => {
    const t = useTranslations("auth.login");
    const [visible, setVisible] = useState(false);
    const [capsLock, setCapsLock] = useState(false);

    const trackCaps = (e: KeyboardEvent<HTMLInputElement>) =>
      setCapsLock(e.getModifierState?.("CapsLock") ?? false);

    return (
      <div className="relative">
        <Input
          ref={ref}
          type={visible ? "text" : "password"}
          className="pr-10"
          hint={capsLock ? t("capsLock") : hint}
          onKeyUp={(e) => {
            trackCaps(e);
            onKeyUp?.(e);
          }}
          onKeyDown={(e) => {
            trackCaps(e);
            onKeyDown?.(e);
          }}
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          className="btn btn-ghost btn-sm btn-icon absolute right-1 top-[27px]"
          aria-label={visible ? t("hidePassword") : t("showPassword")}
          aria-pressed={visible}
        >
          {visible ? <EyeOff size={15} aria-hidden="true" /> : <Eye size={15} aria-hidden="true" />}
        </button>
      </div>
    );
  },
);

PasswordInput.displayName = "PasswordInput";

export { PasswordInput };
