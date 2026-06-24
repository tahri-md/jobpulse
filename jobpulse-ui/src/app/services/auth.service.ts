import { inject, Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ApiResponse,
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserDto,
} from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authUrl = `${environment.apiUrl}/auth`;
  private readonly passwordUrl = `${environment.apiUrl}/password`;

  private readonly tokenKey = 'jobpulse_token';
  private readonly userKey = 'jobpulse_user';

  private _currentUser = signal<UserDto | null>(this.loadUser());
  private _isAuthenticated = computed(() => !!this._currentUser() && !!this.getToken());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = this._isAuthenticated;

  private http = inject(HttpClient);
  private router = inject(Router);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.authUrl}/login`, request).pipe(
      map((response) => response.data),
      tap((auth) => this.handleAuth(auth)),
    );
  }

  register(request: RegisterRequest): Observable<UserDto> {
    return this.http
      .post<ApiResponse<UserDto>>(`${this.authUrl}/register`, request)
      .pipe(map((response) => response.data));
  }

  getMe(): Observable<UserDto> {
    return this.http.get<ApiResponse<UserDto>>(`${this.authUrl}/me`).pipe(
      map((response) => response.data),
      tap((user) => {
        this._currentUser.set(user);
        localStorage.setItem(this.userKey, JSON.stringify(user));
      }),
    );
  }

  verifyEmail(token: string): Observable<ApiResponse<UserDto>> {
    return this.http.post<ApiResponse<UserDto>>(`${this.authUrl}/verify-email`, { token });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<object> {
    return this.http.post<ApiResponse<object>>(`${this.passwordUrl}/forgot-password`, request).pipe(
      map((response) => response.data),
      tap(() => console.log('Forgot password email sent')),
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<object> {
    return this.http.post<ApiResponse<object>>(`${this.passwordUrl}/reset-password`, request).pipe(
      map((response) => response.data),
      tap(() => console.log('Password reset successfully')),
    );
  }

  changePassword(request: ChangePasswordRequest): Observable<object> {
    return this.http.post<ApiResponse<object>>(`${this.passwordUrl}/change-password`, request).pipe(
      map((response) => response.data),
      tap(() => console.log('Password changed successfully')),
    );
  }
  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setAuthResponse(res: AuthResponse): void {
    this.handleAuth(res);
  }

  private handleAuth(res: AuthResponse): void {
    localStorage.setItem(this.tokenKey, res.accessToken);

    const user: UserDto = {
      username: res.user.username,
      email: res.user.email,
      role: res.user.role,
      avatar: res.user.avatar,
      authProviders: res.user.authProviders,
      lastLoginAt: res.user.lastLoginAt,
    };

    localStorage.setItem(this.userKey, JSON.stringify(user));
    this._currentUser.set(user);
  }

  private loadUser(): UserDto | null {
    const data = localStorage.getItem(this.userKey);
    return data ? JSON.parse(data) : null;
  }
}
