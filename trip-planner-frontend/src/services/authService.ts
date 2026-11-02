import { api, saveToken, clearToken } from "./api";
import type { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from "../types/auth";

export async function register(request: RegisterRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/auth/register", request);
  saveToken(data.token);
  return data;
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/auth/login", request);
  saveToken(data.token);
  return data;
}

export async function getCurrentUser(): Promise<UserResponse> {
  const { data } = await api.get<UserResponse>("/auth/me");
  return data;
}

export function logout() {
  clearToken();
}
