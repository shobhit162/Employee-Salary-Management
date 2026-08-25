import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { EmployeeService } from '../../../core/api/employee.service';
import { problemMessage } from '../../../core/api/problem';
import {
  EmployeeFilterOptions,
  EmployeeListItem,
  EmployeeQuery,
  Page,
} from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../shared/pipes/money/money.pipe';

const PAGE_SIZE = 25;

/**
 * The employee list — where the HR Manager actually works.
 *
 * <p>Search, filtering, sorting and paging all happen server-side, so the page
 * behaves the same whether the organisation has ten employees or ten thousand.
 */
@Component({
  selector: 'app-employee-list',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MoneyPipe,
    DecimalPipe,
    TitleCasePipe,
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.css',
})
export class EmployeeListComponent implements OnInit {
  private readonly employees = inject(EmployeeService);
  private readonly route = inject(ActivatedRoute);

  protected readonly columns = [
    { field: 'employeeCode', label: 'Code' },
    { field: 'lastName', label: 'Name' },
    { field: 'countryCode', label: 'Country' },
    { field: 'department', label: 'Department' },
    { field: 'jobTitle', label: 'Job title' },
    { field: 'employmentStatus', label: 'Status' },
  ];

  protected readonly page = signal<Page<EmployeeListItem> | null>(null);
  protected readonly options = signal<EmployeeFilterOptions>({
    countryCodes: [],
    departments: [],
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly query = signal<EmployeeQuery>({
    page: 0,
    size: PAGE_SIZE,
    sortBy: 'lastName',
    direction: 'ASC',
    status: 'ACTIVE',
  });

  protected readonly filters = inject(FormBuilder).nonNullable.group({
    search: '',
    countryCode: '',
    department: '',
    status: 'ACTIVE',
  });

  ngOnInit(): void {
    this.applyQueryParams();

    this.employees.filterOptions().subscribe({
      next: (options) => this.options.set(options),
      error: () => this.options.set({ countryCodes: [], departments: [] }),
    });

    // Debounced so typing in the search box does not fire a request per keystroke.
    this.filters.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      )
      .subscribe(() => {
        const value = this.filters.getRawValue();

        this.query.update((current) => ({
          ...current,
          page: 0,
          search: value.search || undefined,
          countryCode: value.countryCode || undefined,
          department: value.department || undefined,
          status: (value.status || undefined) as EmployeeQuery['status'],
        }));

        this.load();
      });

    this.load();
  }

  protected sortBy(field: string): void {
    this.query.update((current) => ({
      ...current,
      page: 0,
      sortBy: field,
      direction:
        current.sortBy === field && current.direction === 'ASC'
          ? 'DESC'
          : 'ASC',
    }));

    this.load();
  }

  protected goToPage(page: number): void {
    this.query.update((current) => ({ ...current, page }));
    this.load();
  }

  protected ariaSort(field: string): string {
    if (this.query().sortBy !== field) {
      return 'none';
    }

    return this.query().direction === 'ASC' ? 'ascending' : 'descending';
  }

  /** Lets the dashboard drill into a cohort by linking here with filters set. */
  private applyQueryParams(): void {
    const params = this.route.snapshot.queryParamMap;

    this.filters.patchValue(
      {
        countryCode: params.get('countryCode') ?? '',
        department: params.get('department') ?? '',
        status: params.get('status') ?? this.filters.controls.status.value,
      },
      { emitEvent: false },
    );

    this.query.update((current) => ({
      ...current,
      countryCode: params.get('countryCode') ?? undefined,
      department: params.get('department') ?? undefined,
      status:
        (params.get('status') as EmployeeQuery['status']) ?? current.status,
    }));
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.employees.search(this.query()).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading.set(false);
      },
      error: (failure) => {
        this.loading.set(false);
        this.error.set(problemMessage(failure, 'Could not load employees.'));
      },
    });
  }
}
