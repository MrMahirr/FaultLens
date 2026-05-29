import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { HttpMethod } from "@/shared/api/methods";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
}

export const AuthApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient({
      method: HttpMethod.POST,
      url: Endpoints.AUTH.LOGIN,
      data,
    });
    return response.data.data;
  },
};
