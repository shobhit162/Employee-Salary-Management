import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Compensation,
  CompensationSummary,
  CompensationWriteModel,
} from '../models/compensation.model';

@Injectable({ providedIn: 'root' })
export class CompensationService {
  private readonly http = inject(HttpClient);

  summary(employeeId: string): Observable<CompensationSummary> {
    return this.http.get<CompensationSummary>(this.url(employeeId));
  }

  save(
    employeeId: string,
    compensation: CompensationWriteModel,
  ): Observable<Compensation> {
    return this.http.post<Compensation>(this.url(employeeId), compensation);
  }

  cancelScheduled(
    employeeId: string,
    compensationId: string,
  ): Observable<void> {
    return this.http.delete<void>(`${this.url(employeeId)}/${compensationId}`);
  }

  private url(employeeId: string): string {
    return `/api/v1/employees/${employeeId}/compensations`;
  }
}
