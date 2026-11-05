import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement, ReactNode } from "react";
import { useAuthStore } from "@/features/auth/store/auth.store";

/** Fresh client per test: no retries (failures surface at once), no cache sharing. */
export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } },
  });
}

export function queryWrapper(client = createTestQueryClient()) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

export function renderWithQuery(ui: ReactElement, client = createTestQueryClient()) {
  return { client, ...render(ui, { wrapper: queryWrapper(client) }) };
}

/** Unsigned JWT carrying the given role claim (the frontend never verifies signatures). */
export function fakeToken(username: string, role: string) {
  const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${encode({ alg: "none" })}.${encode({ sub: username, roles: [`ROLE_${role}`], exp: 9_999_999_999 })}.`;
}

/** Puts a signed-in user of the given role into the auth store. */
export function signInAs(role: "ADMIN" | "EDITOR" | "VIEWER", username = role.toLowerCase()) {
  useAuthStore.setState({ token: fakeToken(username, role), user: { username, role } });
}
