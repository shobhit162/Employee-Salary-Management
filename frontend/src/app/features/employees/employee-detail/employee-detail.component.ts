import { DatePipe, TitleCasePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CompensationService } from '../../../core/api/compensation.service';
import { EmployeeService } from '../../../core/api/employee.service';
import { problemMessage } from '../../../core/api/problem';
import { CompensationSummary } from '../../../core/models/compensation.model';
import { Employee } from '../../../core/models/employee.model';
import { MoneyPipe } from '../../../shared/pipes/money/money.pipe';
import { SalaryChangeFormComponent } from '../salary-change-form/salary-change-form.component';

/**
 * One employee: their details, what they earn now, what is scheduled, and the
 * full salary timeline behind them.
 */
@Component({
  selector: 'app-employee-detail',
  imports: [
    RouterLink,
    DatePipe,
    TitleCasePipe,
    MoneyPipe,
    SalaryChangeFormComponent,
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.css',
})
export class EmployeeDetailComponent implements OnInit {
  private readonly employees = inject(EmployeeService);
  private readonly compensations = inject(CompensationService);
  private readonly route = inject(ActivatedRoute);

  protected readonly employee = signal<Employee | null>(null);
  protected readonly summary = signal<CompensationSummary | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly terminating = signal(false);
  protected readonly cancelling = signal(false);

  /** Keeps a salary change in the currency the employee is already paid in. */
  protected readonly defaultCurrency = computed(
    () =>
      this.summary()?.current?.currency ??
      this.summary()?.history[0]?.currency ??
      'USD',
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.error.set('No employee selected.');
      return;
    }

    this.employees.getById(id).subscribe({
      next: (employee) => this.employee.set(employee),
      error: (failure) =>
        this.error.set(problemMessage(failure, 'Could not load the employee.')),
    });

    this.loadCompensation(id);
  }

  protected loadCompensation(employeeId: string): void {
    this.compensations.summary(employeeId).subscribe({
      next: (summary) => this.summary.set(summary),
      error: (failure) =>
        this.error.set(
          problemMessage(failure, 'Could not load salary information.'),
        ),
    });
  }

  protected terminate(): void {
    const person = this.employee();

    if (!person) {
      return;
    }

    const confirmed = confirm(
      `Terminate ${person.firstName} ${person.lastName}? ` +
        'Their salary history will be kept.',
    );

    if (!confirmed) {
      return;
    }

    this.terminating.set(true);
    this.error.set(null);

    this.employees.terminate(person.id).subscribe({
      next: (updated) => {
        this.employee.set(updated);
        this.terminating.set(false);
      },
      error: (failure) => {
        this.terminating.set(false);
        this.error.set(
          problemMessage(failure, 'Could not terminate the employee.'),
        );
      },
    });
  }

  protected cancelScheduled(compensationId: string): void {
    const person = this.employee();

    if (!person) {
      return;
    }

    this.cancelling.set(true);
    this.error.set(null);

    this.compensations.cancelScheduled(person.id, compensationId).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.loadCompensation(person.id);
      },
      error: (failure) => {
        this.cancelling.set(false);
        this.error.set(
          problemMessage(failure, 'Could not cancel the scheduled change.'),
        );
      },
    });
  }
}
