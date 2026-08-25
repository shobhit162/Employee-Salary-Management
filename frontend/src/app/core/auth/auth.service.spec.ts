import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

const SESSION_KEY = 'acme.salary.session';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  function configure(): void {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  }

  function storedSession(expiresAt: string): void {
    localStorage.setItem(
      SESSION_KEY,
      JSON.stringify({
        token: 'stored-token',
        expiresAt,
        username: 'hr.manager@acme.com',
      }),
    );
  }

  function inHours(hours: number): string {
    return new Date(Date.now() + hours * 3_600_000).toISOString();
  }

  afterEach(() => {
    localStorage.removeItem(SESSION_KEY);
    TestBed.resetTestingModule();
  });

  it('starts signed out when nothing is stored', () => {
    localStorage.removeItem(SESSION_KEY);
    configure();

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.token()).toBeNull();
  });

  it('stores the session after a successful sign-in', () => {
    localStorage.removeItem(SESSION_KEY);
    configure();

    service.login('hr.manager@acme.com', 'secret').subscribe();

    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({
      username: 'hr.manager@acme.com',
      password: 'secret',
    });

    request.flush({
      token: 'issued-token',
      expiresAt: inHours(8),
      username: 'hr.manager@acme.com',
      roles: ['HR_MANAGER'],
    });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.username()).toBe('hr.manager@acme.com');
    expect(service.token()).toBe('issued-token');

    http.verify();
  });

  it('restores a session that is still valid, so a refresh does not sign the user out', () => {
    storedSession(inHours(4));
    configure();

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token()).toBe('stored-token');
  });

  it('discards a stored session that has already expired', () => {
    storedSession(inHours(-1));
    configure();

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('discards a corrupted stored session instead of crashing on load', () => {
    localStorage.setItem(SESSION_KEY, 'not-json');
    configure();

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('signs the user out when a restored token expires mid-session', () => {
    // Restored while valid, then read after it lapses.
    storedSession(new Date(Date.now() + 50).toISOString());
    configure();
    expect(service.isAuthenticated()).toBeTrue();

    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(Date.now() + 60_000));

    expect(service.token()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);

    jasmine.clock().uninstall();
  });

  it('clears the session and returns to login on sign-out', () => {
    storedSession(inHours(4));
    configure();

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
