import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AnalyticsService } from '../../../core/api/analytics.service';
import { EmployeeService } from '../../../core/api/employee.service';
import { problemMessage } from '../../../core/api/problem';
import {
  AnalyticsFilters,
  BreakdownDimension,
  SalaryBreakdown,
  SalaryDistribution,
  SalarySummary,
} from '../../../core/models/analytics.model';
import { EmployeeFilterOptions } from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../shared/pipes/money/money.pipe';
import { BarChartComponent, BarDatum } from '../bar-chart/bar-chart.component';
import { HistogramComponent } from '../histogram/histogram.component';

const BAND_SIZE = 25_000;

/**
 * The compensation dashboard.
 *
 * <p>Every panel answers to the same filter bar, so "how do we pay engineers in
 * India?" is the same interaction as "how do we pay the company?". The three
 * queries are issued in parallel, and the breakdown can be re-fetched on its own
 * when the dimension is toggled.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DecimalPipe,
    MoneyPipe,
    BarChartComponent,
    HistogramComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);
  private readonly employees = inject(EmployeeService);

  protected readonly bandSize = BAND_SIZE;

  protected readonly summary = signal<SalarySummary | null>(null);
  protected readonly breakdown = signal<SalaryBreakdown | null>(null);
  protected readonly distribution = signal<SalaryDistribution | null>(null);
  protected readonly options = signal<EmployeeFilterOptions>({
    countryCodes: [],
    departments: [],
  });
  protected readonly currencies = signal<string[]>(['USD']);
  protected readonly dimension = signal<BreakdownDimension>('COUNTRY');
  protected readonly error = signal<string | null>(null);

  protected readonly filters = signal<AnalyticsFilters>({
    countryCode: null,
    department: null,
    status: 'ACTIVE',
    currency: 'USD',
  });

  /** Headcount carrying no salary — a data gap worth surfacing, not hiding. */
  protected readonly missingSalaryCount = computed(() => {
    const statistics = this.summary()?.statistics;

    return statistics
      ? statistics.employeeCount - statistics.compensatedEmployeeCount
      : 0;
  });

  protected readonly breakdownBars = computed<BarDatum[]>(
    () =>
      this.breakdown()?.rows.map((row) => ({
        label: row.key,
        value: row.statistics.totalAnnualCompensation,
        caption: `${row.statistics.employeeCount} employees`,
      })) ?? [],
  );

  protected readonly form = inject(FormBuilder).nonNullable.group({
    countryCode: '',
    department: '',
    status: 'ACTIVE',
    currency: 'USD',
  });

  ngOnInit(): void {
    this.employees.filterOptions().subscribe({
      next: (options) => this.options.set(options),
      error: () => this.options.set({ countryCodes: [], departments: [] }),
    });

    this.analytics.reportingCurrencies().subscribe({
      next: (currencies) => this.currencies.set(currencies),
      error: () => this.currencies.set(['USD']),
    });

    this.form.valueChanges.subscribe(() => {
      const value = this.form.getRawValue();

      this.filters.set({
        countryCode: value.countryCode || null,
        department: value.department || null,
        status: value.status as AnalyticsFilters['status'],
        currency: value.currency,
      });

      this.load();
    });

    this.load();
  }

  protected setDimension(dimension: BreakdownDimension): void {
    if (this.dimension() === dimension) {
      return;
    }

    this.dimension.set(dimension);
    this.loadBreakdown();
  }

  /** Carries the current cohort into the employee list. */
  protected drillDownParams(key: string): Record<string, string> {
    const filters = this.filters();
    const status = filters.status === 'ALL' ? '' : filters.status;

    return this.dimension() === 'COUNTRY'
      ? { countryCode: key, status }
      : { department: key, status };
  }

  private load(): void {
    this.error.set(null);

    const filters = this.filters();

    forkJoin({
      summary: this.analytics.summary(filters),
      breakdown: this.analytics.breakdown(filters, this.dimension()),
      distribution: this.analytics.distribution(filters, BAND_SIZE),
    }).subscribe({
      next: (result) => {
        this.summary.set(result.summary);
        this.breakdown.set(result.breakdown);
        this.distribution.set(result.distribution);
      },
      error: (failure) =>
        this.error.set(problemMessage(failure, 'Could not load analytics.')),
    });
  }

  private loadBreakdown(): void {
    this.analytics.breakdown(this.filters(), this.dimension()).subscribe({
      next: (breakdown) => this.breakdown.set(breakdown),
      error: (failure) =>
        this.error.set(problemMessage(failure, 'Could not load the breakdown.')),
    });
  }
}
