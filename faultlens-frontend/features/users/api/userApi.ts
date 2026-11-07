import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";
import type { Role } from "@/features/auth/hooks/useCan";
import type { CreateUserInput, ManagedUser } from "../types/user.types";

/** User management REST client (/api/v1/users, ADMIN only). */
export const UserApi = {
  list: async (): Promise<ManagedUser[]> => {
    const response = await apiClient({ method: HttpMethod.GET, url: Endpoints.USERS.LIST });
    return response.data.data;
  },

  create: async (input: CreateUserInput): Promise<ManagedUser> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.USERS.LIST,
      data: input,
    });
    return response.data.data;
  },

  changeRole: async (id: number, role: Role): Promise<ManagedUser> => {
    const response = await apiClient({
      method: HttpMethod.PUT,
      url: Endpoints.USERS.ROLE(id),
      data: { role },
    });
    return response.data.data;
  },

  resetPassword: async (id: number, password: string): Promise<ManagedUser> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.USERS.RESET_PASSWORD(id),
      data: { password },
    });
    return response.data.data;
  },

  remove: async (id: number): Promise<void> => {
    await apiClient({ method: HttpMethod.DELETE, url: Endpoints.USERS.DETAIL(id) });
  },
};
