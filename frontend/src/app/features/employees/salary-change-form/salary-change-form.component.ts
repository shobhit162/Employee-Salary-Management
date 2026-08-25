import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CompensationService } from '../../../core/api/compensation.service';
import { problemMessage } from '../../../core/api/problem';

/**
 * Sets or changes an employee's salary.
 *
 * <p>The form states the rule the server enforces before the HR Manager can hit
 * it: a first salary may start today, a change to an existing one must be dated
 * in the future, and only one change may be pending at a time.
 */
@Component({
  selector: 'app-salary-change-form',
  imports: [ReactiveFormsModule],
  templateUrl: './salary-change-form.component.html',
  styleUrl: './salary-change-form.component.css',
})
export class SalaryChangeFormComponent {
  readonly employeeId = input.required<string>();
  readonly hasCurrentSalary = input(false);
  readonly hasScheduledChange = input(false);
  readonly defaultCurrency = input('USD');

  readonly saved = output<void>();

  private readonly compensations = inject(CompensationService);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Today for a first salary, tomorrow for a change to an existing one. */
  protected readonly earliestDate = computed(() => {
    const date = new Date();

    if (this.hasCurrentSalary()) {
      date.setDate(date.getDate() + 1);
    }

    return toIsoDate(date);
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    currency: [
      'USD',
      [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)],
    ],
    effectiveFrom: ['', Validators.required],
  });

  constructor() {
    // Prefill from the employee's existing pay, and follow it if the parent
    // reloads the summary, so the common case is a single field to fill in.
    effect(() => {
      this.form.patchValue({
        currency: this.defaultCurrency(),
        effectiveFrom: this.earliestDate(),
      });
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.busy.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();

    this.compensations
      .save(this.employeeId(), {
        amount: Number(value.amount),
        currency: value.currency.toUpperCase(),
        effectiveFrom: value.effectiveFrom,
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.form.controls.amount.reset(null);
          this.saved.emit();
        },
        error: (failure) => {
          this.busy.set(false);
          this.error.set(
            problemMessage(failure, 'Could not save the salary change.'),
          );
        },
      });
  }
}

function toIsoDate(date: Date): string {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
}
