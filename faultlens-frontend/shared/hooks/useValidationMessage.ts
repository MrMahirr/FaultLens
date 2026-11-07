"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { MESSAGES } from "@/messages";

type ValidationKey = keyof (typeof MESSAGES)["tr"]["validation"];

function isValidationKey(value: string): value is ValidationKey {
  return value in MESSAGES.tr.validation;
}

/**
 * Form schemas (zod) carry keys of the "validation" namespace as their messages; this turns
 * such a key into text in the current language. Anything else (zod's built-in messages, server
 * errors) is shown as is.
 */
export function useValidationMessage() {
  const t = useTranslations("validation");
  return useCallback(
    (message: string | undefined) =>
      message === undefined ? undefined : isValidationKey(message) ? t(message) : message,
    [t],
  );
}
