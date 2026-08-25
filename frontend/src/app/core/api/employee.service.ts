import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Employee,
  EmployeeFilterOptions,
  EmployeeListItem,
  EmployeeQuery,
  EmployeeWriteModel,
  Page,
} from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/employees';

  search(query: EmployeeQuery): Observable<Page<EmployeeListItem>> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sortBy', query.sortBy)
      .set('direction', query.direction);

    // Empty filters are omitted so the server keeps its "no filter" meaning.
    if (query.search) {
      params = params.set('search', query.search);
    }
    if (query.countryCode) {
      params = params.set('countryCode', query.countryCode);
    }
    if (query.department) {
      params = params.set('department', query.department);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }

    return this.http.get<Page<EmployeeListItem>>(this.baseUrl, { params });
  }

  getById(employeeId: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/${employeeId}`);
  }

  filterOptions(): Observable<EmployeeFilterOptions> {
    return this.http.get<EmployeeFilterOptions>(
      `${this.baseUrl}/filter-options`,
    );
  }

  create(employee: EmployeeWriteModel): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, employee);
  }

  update(
    employeeId: string,
    employee: EmployeeWriteModel,
  ): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${employeeId}`, employee);
  }

  terminate(employeeId: string): Observable<Employee> {
    return this.http.post<Employee>(
      `${this.baseUrl}/${employeeId}/termination`,
      {},
    );
  }
}
