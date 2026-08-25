import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AnalyticsFilters,
  BreakdownDimension,
  SalaryBreakdown,
  SalaryDistribution,
  SalarySummary,
} from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/analytics';

  summary(filters: AnalyticsFilters): Observable<SalarySummary> {
    return this.http.get<SalarySummary>(`${this.baseUrl}/summary`, {
      params: this.toParams(filters),
    });
  }

  breakdown(
    filters: AnalyticsFilters,
    dimension: BreakdownDimension,
  ): Observable<SalaryBreakdown> {
    return this.http.get<SalaryBreakdown>(`${this.baseUrl}/breakdown`, {
      params: this.toParams(filters).set('dimension', dimension),
    });
  }

  distribution(
    filters: AnalyticsFilters,
    bandSize: number,
  ): Observable<SalaryDistribution> {
    return this.http.get<SalaryDistribution>(`${this.baseUrl}/distribution`, {
      params: this.toParams(filters).set('bandSize', bandSize),
    });
  }

  reportingCurrencies(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/currencies`);
  }

  private toParams(filters: AnalyticsFilters): HttpParams {
    let params = new HttpParams()
      .set('status', filters.status)
      .set('currency', filters.currency);

    if (filters.countryCode) {
      params = params.set('countryCode', filters.countryCode);
    }
    if (filters.department) {
      params = params.set('department', filters.department);
    }

    return params;
  }
}
