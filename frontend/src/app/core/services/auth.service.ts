import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  role: string;
}

export interface CurrentUser {
  sub: string;
  email: string;
  role: string;
  tenantId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiUrl}/auth`;

  private readonly _accessToken = signal<string | null>(localStorage.getItem('accessToken'));
  private readonly _currentUser = signal<CurrentUser | null>(
    this.parseToken(localStorage.getItem('accessToken'))
  );

  readonly isAuthenticated = computed(() => !!this._accessToken());
  readonly currentUser = this._currentUser.asReadonly();

  get accessToken(): string | null { return this._accessToken(); }
  get refreshToken(): string | null { return localStorage.getItem('refreshToken'); }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, { email, password }).pipe(
      tap(res => this.storeTokens(res))
    );
  }

  refresh(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.base}/refresh`,
      { refreshToken: this.refreshToken }
    ).pipe(tap(res => this.storeTokens(res)));
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    if (refreshToken) {
      this.http.post(`${this.base}/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
    this.clearStorage();
    this.router.navigate(['/login']);
  }

  clearStorage(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this._accessToken.set(null);
    this._currentUser.set(null);
  }

  private storeTokens(res: LoginResponse): void {
    localStorage.setItem('accessToken', res.accessToken);
    localStorage.setItem('refreshToken', res.refreshToken);
    this._accessToken.set(res.accessToken);
    this._currentUser.set(this.parseToken(res.accessToken));
  }

  private parseToken(token: string | null): CurrentUser | null {
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      return {
        sub: decoded.sub ?? '',
        email: decoded.email ?? decoded.sub ?? '',
        role: decoded.role ?? '',
        tenantId: decoded.tenantId ?? decoded.tenant_id ?? '',
      };
    } catch {
      return null;
    }
  }
}