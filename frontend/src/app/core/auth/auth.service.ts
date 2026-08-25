import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

interface LoginResponse {
  token: string;
  expiresAt: string;
  username: string;
  roles: string[];
}

interface StoredSession {
  token: string;
  expiresAt: string;
  username: string;
}

const SESSION_KEY = 'acme.salary.session';

/**
 * Holds the signed-in HR Manager's session.
 *
 * <p>The token is kept in localStorage so a page refresh does not log the user
 * out. That is a deliberate trade-off: it is readable by scripts running on the
 * page, which is acceptable here because the app has no third-party scripts and
 * the token is short-lived. An httpOnly cookie would be the stricter choice but
 * would mean giving up the stateless API and adding CSRF protection.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<StoredSession | null>(this.restore());

  readonly username = computed(() => this.session()?.username ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/v1/auth/login', { username, password })
      .pipe(tap((response) => this.store(response)));
  }

  logout(): void {
    localStorage.removeItem(SESSION_KEY);
    this.session.set(null);
    void this.router.navigate(['/login']);
  }

  token(): string | null {
    const session = this.session();

    if (!session) {
      return null;
    }

    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      this.logout();
      return null;
    }

    return session.token;
  }

  private store(response: LoginResponse): void {
    const session: StoredSession = {
      token: response.token,
      expiresAt: response.expiresAt,
      username: response.username,
    };

    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private restore(): StoredSession | null {
    const raw = localStorage.getItem(SESSION_KEY);

    if (!raw) {
      return null;
    }

    try {
      const session = JSON.parse(raw) as StoredSession;

      if (new Date(session.expiresAt).getTime() <= Date.now()) {
        localStorage.removeItem(SESSION_KEY);
        return null;
      }

      return session;
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
