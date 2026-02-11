import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface GmailStatus {
  gmailAddress: string | null;
  connected: boolean;
}

@Injectable({ providedIn: 'root' })
export class GmailService {
  private readonly apiUrl = `${environment.apiUrl}/gmail`;

  constructor(private http: HttpClient) {}

  getAuthUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.apiUrl}/auth-url`);
  }

  exchangeCode(code: string): Observable<GmailStatus> {
    return this.http.post<GmailStatus>(`${this.apiUrl}/callback`, { code });
  }

  getStatus(): Observable<GmailStatus> {
    return this.http.get<GmailStatus>(`${this.apiUrl}/status`);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/disconnect`);
  }
}
