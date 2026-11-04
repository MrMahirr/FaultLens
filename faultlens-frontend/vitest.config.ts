import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": fileURLToPath(new URL("./", import.meta.url)) },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["**/*.test.{ts,tsx}"],
    exclude: ["node_modules", ".next"],
    // Keep fixed so Endpoints.* (inlined from env at import time) match the MSW handlers.
    env: {
      NEXT_PUBLIC_API_URL: "http://localhost:3500/api/v1",
      NEXT_PUBLIC_WS_URL: "ws://localhost:3500/ws",
    },
    coverage: {
      provider: "v8",
      reporter: ["text-summary", "html", "lcov"],
      include: ["{app,features,shared,design-system}/**/*.{ts,tsx}"],
      exclude: ["**/*.test.{ts,tsx}", "**/*.d.ts"],
    },
  },
});
