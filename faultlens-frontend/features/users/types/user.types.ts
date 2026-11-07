import type { Role } from "@/features/auth/hooks/useCan";

export interface ManagedUser {
  id: number;
  username: string;
  email: string | null;
  role: Role;
  mustChangePassword: boolean;
  createdAt: string | null;
}

export interface CreateUserInput {
  username: string;
  email?: string;
  role: Role;
  password: string;
}
