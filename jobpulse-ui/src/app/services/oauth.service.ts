import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from '../models';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class OAuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth/oauth`;

  constructor(
    private http: HttpClient,
    private router: Router,
    private authService: AuthService
  ) {}

  /**
   * Initiates Google OAuth by redirecting to Google's consent page.
   * Uses authorization code flow with gmail.send scope so the backend
   * can also store Gmail tokens for sending emails.
   */
  signInWithGoogle(): void {
    const clientId = environment.googleClientId;
    const redirectUri = `${window.location.origin}/google/callback`;
    const scope = 'openid email profile https://www.googleapis.com/auth/gmail.send';
    const state = this.generateState();

    sessionStorage.setItem('google_oauth_state', state);

    window.location.href =
      `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=${encodeURIComponent(scope)}&access_type=offline&prompt=consent&state=${state}`;
  }

  /**
   * Sends the Google authorization code to the backend.
   */
  loginWithGoogle(code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/google`, { code }).pipe(
      tap(res => this.authService.setAuthResponse(res)),
      catchError(err => throwError(() => err))
    );
  }

  /**
   * Initiates GitHub OAuth flow by redirecting to GitHub's authorize page.
   */
  signInWithGitHub(): void {
    const clientId = environment.githubClientId;
    const redirectUri = `${window.location.origin}/github/callback`;
    const scope = 'read:user user:email';
    const state = this.generateState();

    // Store state to verify later
    sessionStorage.setItem('github_oauth_state', state);

    window.location.href =
      `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=${encodeURIComponent(scope)}&state=${state}`;
  }

  /**
   * Sends the GitHub authorization code to the backend and processes the auth response.
   */
  loginWithGitHub(code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/github`, { code }).pipe(
      tap(res => this.authService.setAuthResponse(res)),
      catchError(err => throwError(() => err))
    );
  }

  /**
   * Verifies the state parameter from the GitHub callback.
   */
  verifyState(state: string): boolean {
    const stored = sessionStorage.getItem('github_oauth_state');
    sessionStorage.removeItem('github_oauth_state');
    return stored === state;
  }

  /**
   * Verifies the state parameter from the Google callback.
   */
  verifyGoogleState(state: string): boolean {
    const stored = sessionStorage.getItem('google_oauth_state');
    sessionStorage.removeItem('google_oauth_state');
    return stored === state;
  }

  private generateState(): string {
    const array = new Uint8Array(16);
    crypto.getRandomValues(array);
    return Array.from(array, b => b.toString(16).padStart(2, '0')).join('');
  }
}
