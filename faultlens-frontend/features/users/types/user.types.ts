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

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: "Yönetici",
  EDITOR: "Editör",
  VIEWER: "İzleyici",
};

export const ROLE_DESCRIPTIONS: Record<Role, string> = {
  ADMIN: "Her şey: silme, sistem ayarları, kullanıcı yönetimi",
  EDITOR: "Kaynak/kural düzenler, alarmları onaylar/çözer",
  VIEWER: "Yalnızca görüntüler",
};
