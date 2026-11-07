"use client";

import { useAuthStore } from "@/features/auth/store/auth.store";
import { decodeJwt } from "@/shared/lib/jwt";

export type Role = "ADMIN" | "EDITOR" | "VIEWER";

/**
 * What a role may do; mirrors the backend's {@code Roles} / {@code @PreAuthorize} rules.
 * The UI only hides controls — the API enforces the same rules.
 */
export type Permission = "edit" | "delete" | "manageUsers" | "manageSystem";

const PERMISSIONS: Record<Role, ReadonlySet<Permission>> = {
  ADMIN: new Set(["edit", "delete", "manageUsers", "manageSystem"]),
  EDITOR: new Set(["edit"]),
  VIEWER: new Set(),
};

/** The role the API sees: the token's claim, falling back to the stored user. Unknown → VIEWER. */
export function roleOf(token: string | null, storedRole: string | undefined): Role {
  const claim = token ? decodeJwt(token)?.roles?.[0]?.replace("ROLE_", "") : undefined;
  const role = claim ?? storedRole;
  return role === "ADMIN" || role === "EDITOR" ? role : "VIEWER";
}

export function can(role: Role, permission: Permission): boolean {
  return PERMISSIONS[role].has(permission);
}

/** `const canDelete = useCan("delete")` */
export function useCan(permission: Permission): boolean {
  const token = useAuthStore((state) => state.token);
  const storedRole = useAuthStore((state) => state.user?.role);
  return can(roleOf(token, storedRole), permission);
}

export function useRole(): Role {
  const token = useAuthStore((state) => state.token);
  const storedRole = useAuthStore((state) => state.user?.role);
  return roleOf(token, storedRole);
}
