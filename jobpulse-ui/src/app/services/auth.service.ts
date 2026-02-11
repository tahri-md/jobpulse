import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private readonly tokenKey = 'jobpulse_token';
  private readonly userKey = 'jobpulse_user';

  private _currentUser = signal<UserResponse | null>(this.loadUser());
  private _isAuthenticated = computed(() => !!this._currentUser() && !!this.getToken());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = this._isAuthenticated;

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(res => this.handleAuth(res)),
      catchError(err => throwError(() => err))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(res => this.handleAuth(res)),
      catchError(err => throwError(() => err))
    );
  }

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`).pipe(
      tap(user => {
        this._currentUser.set(user);
        localStorage.setItem(this.userKey, JSON.stringify(user));
      })
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

  /** Called externally (e.g. by OAuthService) to store auth data and update state. */
  setAuthResponse(res: AuthResponse): void {
    this.handleAuth(res);
  }

  private handleAuth(res: AuthResponse): void {
    localStorage.setItem(this.tokenKey, res.accessToken);
    const user: UserResponse = {
      id: res.user.id,
      username: res.user.username,
      email: res.user.email,
      role: res.user.role
    };
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this._currentUser.set(user);
  }

  private loadUser(): UserResponse | null {
    const data = localStorage.getItem(this.userKey);
    return data ? JSON.parse(data) : null;
  }
}
