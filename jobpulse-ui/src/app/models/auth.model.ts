import { Role, AuthProvider } from './enums';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  authProvider: AuthProvider;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: string;
}
