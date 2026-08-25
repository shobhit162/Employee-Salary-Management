import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SalaryChangeFormComponent } from './salary-change-form.component';

/**
 * The form mirrors a rule the server enforces: a first salary may start today,
 * a change to an existing one may not. If the two drift apart, the HR Manager
 * gets a rejection they were given no warning about.
 *
 * <p>Driven through the DOM rather than by calling methods, so the component's
 * template-only members stay `protected` and the test exercises what a user
 * actually does.
 */
describe('SalaryChangeFormComponent', () => {
  let fixture: ComponentFixture<SalaryChangeFormComponent>;
  let http: HttpTestingController;

  const employeeId = 'e1f2a3b4-0000-0000-0000-000000000001';
  const compensationsUrl = `/api/v1/employees/${employeeId}/compensations`;

  function isoDaysFromToday(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);

    return [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-');
  }

  function render(inputs: Record<string, unknown> = {}): void {
    fixture = TestBed.createComponent(SalaryChangeFormComponent);
    fixture.componentRef.setInput('employeeId', employeeId);

    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );

    fixture.detectChanges();
  }

  function query<T extends HTMLElement>(selector: string): T {
    return fixture.nativeElement.querySelector(selector) as T;
  }

  function type(selector: string, value: string): void {
    const field = query<HTMLInputElement>(selector);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function fillIn(amount: string, currency: string, effectiveFrom: string): void {
    type('input[type="number"]', amount);
    type('input[formControlName="currency"]', currency);
    type('input[type="date"]', effectiveFrom);
  }

  function submit(): void {
    query<HTMLFormElement>('form').dispatchEvent(
      new Event('submit', { cancelable: true }),
    );
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('allows a first salary to start today', () => {
    render({ hasCurrentSalary: false });

    expect(query<HTMLInputElement>('input[type="date"]').min).toBe(
      isoDaysFromToday(0),
    );
    expect(fixture.nativeElement.textContent).toContain(
      'can take effect from today',
    );
  });

  it('requires a change to an existing salary to start tomorrow at the earliest', () => {
    render({ hasCurrentSalary: true });

    expect(query<HTMLInputElement>('input[type="date"]').min).toBe(
      isoDaysFromToday(1),
    );
    expect(fixture.nativeElement.textContent).toContain('future date');
  });

  it('prefills the currency the employee is already paid in', () => {
    render({ hasCurrentSalary: true, defaultCurrency: 'GBP' });

    expect(query<HTMLInputElement>('input[formControlName="currency"]').value)
      .toBe('GBP');
  });

  it('offers no form while a change is already pending', () => {
    // The server allows one pending change; the UI says so rather than letting
    // the HR Manager fill in a form that is going to be rejected.
    render({ hasCurrentSalary: true, hasScheduledChange: true });

    expect(query('form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('already scheduled');
  });

  it('submits an uppercase currency and a numeric amount', () => {
    render({ hasCurrentSalary: false, defaultCurrency: 'eur' });

    fillIn('95000', 'eur', isoDaysFromToday(0));
    submit();

    const request = http.expectOne(compensationsUrl);
    expect(request.request.body).toEqual({
      amount: 95000,
      currency: 'EUR',
      effectiveFrom: isoDaysFromToday(0),
    });

    request.flush({});
  });

  it('does not submit an incomplete form', () => {
    render({ hasCurrentSalary: false });

    // Currency and date are prefilled; without an amount there is nothing to save.
    type('input[formControlName="currency"]', 'USD');
    submit();

    expect(http.match(compensationsUrl)).toHaveSize(0);
    expect(query<HTMLButtonElement>('button[type="submit"]').disabled).toBeTrue();
  });

  it('shows the reason the server refused the change', async () => {
    render({ hasCurrentSalary: true });

    fillIn('120000', 'USD', isoDaysFromToday(30));
    submit();

    http.expectOne(compensationsUrl).flush(
      {
        title: 'Business Rule Violation',
        detail: 'Employee already has a scheduled compensation change.',
      },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'already has a scheduled compensation change',
    );
  });
});
