import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "./test/msw/server";

// Node 25+ ships its own `localStorage` global, which is `undefined` unless Node runs with
// --localstorage-file, and it shadows jsdom's. Put jsdom's Storage back (no-op on Node 20/22).
const jsdomWindow = (globalThis as { jsdom?: { window: Window } }).jsdom?.window;
for (const key of ["localStorage", "sessionStorage"] as const) {
  if (jsdomWindow && globalThis[key] === undefined) {
    Object.defineProperty(globalThis, key, { value: jsdomWindow[key], configurable: true, writable: true });
  }
}

// Any request without a handler fails the test instead of silently hitting the network.
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  server.resetHandlers();
  cleanup();
  localStorage.clear();
});
afterAll(() => server.close());
