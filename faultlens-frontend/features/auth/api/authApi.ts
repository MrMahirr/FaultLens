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
  /** True when the user must choose a new password before using the app. */
  mustChangePassword: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
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

  /** Changes the password; the response carries a fresh, unrestricted token. */
  changePassword: async (data: ChangePasswordRequest): Promise<LoginResponse> => {
    const response = await apiClient({
      method: HttpMethod.PUT,
      url: Endpoints.AUTH.CHANGE_PASSWORD,
      data,
    });
    return response.data.data;
  },
};
