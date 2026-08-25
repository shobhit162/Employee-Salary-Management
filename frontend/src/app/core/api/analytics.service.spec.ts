import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AnalyticsFilters } from '../models/analytics.model';
import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let http: HttpTestingController;

  const activeInUsd: AnalyticsFilters = {
    countryCode: null,
    department: null,
    status: 'ACTIVE',
    currency: 'USD',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AnalyticsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('always states the cohort and the reporting currency', () => {
    service.summary(activeInUsd).subscribe();

    const request = http.expectOne((r) => r.url === '/api/v1/analytics/summary');

    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('currency')).toBe('USD');
    expect(request.request.params.has('countryCode')).toBeFalse();

    request.flush({});
  });

  it('narrows the cohort when a country or department is chosen', () => {
    service
      .summary({
        ...activeInUsd,
        countryCode: 'IN',
        department: 'Engineering',
      })
      .subscribe();

    const request = http.expectOne((r) => r.url === '/api/v1/analytics/summary');

    expect(request.request.params.get('countryCode')).toBe('IN');
    expect(request.request.params.get('department')).toBe('Engineering');

    request.flush({});
  });

  it('carries the shared filters into the breakdown alongside the dimension', () => {
    service.breakdown(activeInUsd, 'DEPARTMENT').subscribe();

    const request = http.expectOne(
      (r) => r.url === '/api/v1/analytics/breakdown',
    );

    expect(request.request.params.get('dimension')).toBe('DEPARTMENT');
    expect(request.request.params.get('currency')).toBe('USD');

    request.flush({});
  });

  it('carries the shared filters into the distribution alongside the band size', () => {
    service.distribution(activeInUsd, 25000).subscribe();

    const request = http.expectOne(
      (r) => r.url === '/api/v1/analytics/distribution',
    );

    expect(request.request.params.get('bandSize')).toBe('25000');
    expect(request.request.params.get('status')).toBe('ACTIVE');

    request.flush({});
  });

  it('passes ALL through, so the caller can include leavers', () => {
    service.summary({ ...activeInUsd, status: 'ALL' }).subscribe();

    const request = http.expectOne((r) => r.url === '/api/v1/analytics/summary');
    expect(request.request.params.get('status')).toBe('ALL');

    request.flush({});
  });
});
