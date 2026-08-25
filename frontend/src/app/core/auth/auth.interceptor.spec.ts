import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', [
      'token',
      'logout',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });

    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('attaches the bearer token to API calls', () => {
    auth.token.and.returnValue('a-token');

    http.get('/api/v1/employees').subscribe();

    const request = controller.expectOne('/api/v1/employees');
    expect(request.request.headers.get('Authorization')).toBe('Bearer a-token');
    request.flush({});
  });

  it('does not send a token to the login endpoint', () => {
    // Signing in with a stale token attached would be nonsense.
    auth.token.and.returnValue('a-token');

    http.post('/api/v1/auth/login', {}).subscribe();

    const request = controller.expectOne('/api/v1/auth/login');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('sends no header when there is no session', () => {
    auth.token.and.returnValue(null);

    http.get('/api/v1/employees').subscribe();

    const request = controller.expectOne('/api/v1/employees');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('signs the user out when the server rejects the token', () => {
    // An expired session should become a login screen, not a wall of failures.
    auth.token.and.returnValue('expired-token');

    http.get('/api/v1/employees').subscribe({ error: () => undefined });

    controller
      .expectOne('/api/v1/employees')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalled();
  });

  it('does not sign the user out when login itself is rejected', () => {
    // A wrong password is not an expired session.
    auth.token.and.returnValue(null);

    http.post('/api/v1/auth/login', {}).subscribe({ error: () => undefined });

    controller
      .expectOne('/api/v1/auth/login')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('leaves other failures to the caller', () => {
    auth.token.and.returnValue('a-token');

    http.get('/api/v1/employees').subscribe({ error: () => undefined });

    controller
      .expectOne('/api/v1/employees')
      .flush({}, { status: 500, statusText: 'Server Error' });

    expect(auth.logout).not.toHaveBeenCalled();
  });
});
