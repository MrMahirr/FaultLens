"use client";

import { Toaster } from "react-hot-toast";

/**
 * The app's only notification surface (react-hot-toast). Confirmations use ConfirmDialog.
 * Styled like Instrument `.toast`: raised surface, hairline + --pop, 13px text.
 */
export function AppToaster() {
  return (
    <Toaster
      position="bottom-right"
      gutter={8}
      toastOptions={{
        duration: 4000,
        style: {
          background: "var(--raised)",
          color: "var(--fg)",
          boxShadow: "var(--pop)",
          borderRadius: "var(--r-md)",
          fontSize: "13px",
          padding: "8px 12px",
        },
        success: { iconTheme: { primary: "var(--ok)", secondary: "var(--raised)" } },
        error: { iconTheme: { primary: "var(--crit)", secondary: "var(--raised)" } },
      }}
    />
  );
}
