import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { EmployeeQuery } from '../models/employee.model';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let http: HttpTestingController;

  const baseQuery: EmployeeQuery = {
    page: 0,
    size: 25,
    sortBy: 'lastName',
    direction: 'ASC',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(EmployeeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('always sends paging and sorting', () => {
    service.search(baseQuery).subscribe();

    const request = http.expectOne((r) => r.url === '/api/v1/employees');
    const params = request.request.params;

    expect(params.get('page')).toBe('0');
    expect(params.get('size')).toBe('25');
    expect(params.get('sortBy')).toBe('lastName');
    expect(params.get('direction')).toBe('ASC');

    request.flush({ content: [] });
  });

  it('omits filters that are not set', () => {
    // An empty string is not "no filter" to the server — it would be matched
    // literally — so unset filters must not be sent at all.
    service.search(baseQuery).subscribe();

    const request = http.expectOne((r) => r.url === '/api/v1/employees');
    const params = request.request.params;

    expect(params.has('search')).toBeFalse();
    expect(params.has('countryCode')).toBeFalse();
    expect(params.has('department')).toBeFalse();
    expect(params.has('status')).toBeFalse();

    request.flush({ content: [] });
  });

  it('sends the filters that are set', () => {
    service
      .search({
        ...baseQuery,
        search: 'ada',
        countryCode: 'GB',
        department: 'Engineering',
        status: 'TERMINATED',
      })
      .subscribe();

    const request = http.expectOne((r) => r.url === '/api/v1/employees');
    const params = request.request.params;

    expect(params.get('search')).toBe('ada');
    expect(params.get('countryCode')).toBe('GB');
    expect(params.get('department')).toBe('Engineering');
    expect(params.get('status')).toBe('TERMINATED');

    request.flush({ content: [] });
  });

  it('terminates through a dedicated sub-resource, never a DELETE', () => {
    // Employees are never removed; termination is a lifecycle transition.
    service.terminate('abc-123').subscribe();

    const request = http.expectOne('/api/v1/employees/abc-123/termination');
    expect(request.request.method).toBe('POST');

    request.flush({});
  });
});
