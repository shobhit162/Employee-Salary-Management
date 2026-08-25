import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { EmployeeService } from '../../../core/api/employee.service';
import { problemMessage } from '../../../core/api/problem';

/**
 * Creates a new employee, or edits an existing one.
 *
 * <p>The two cases differ in one field only — the employee code is fixed once
 * assigned, because it is how the rest of the organisation refers to a person.
 */
@Component({
  selector: 'app-employee-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.css',
})
export class EmployeeFormComponent implements OnInit {
  private readonly employees = inject(EmployeeService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly employeeId = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    employeeCode: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    countryCode: [
      '',
      [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)],
    ],
    department: ['', [Validators.required, Validators.maxLength(100)]],
    jobTitle: ['', [Validators.required, Validators.maxLength(150)]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      return;
    }

    this.employeeId.set(id);
    this.form.controls.employeeCode.disable();

    this.employees.getById(id).subscribe({
      next: (employee) => this.form.patchValue(employee),
      error: (failure) =>
        this.error.set(problemMessage(failure, 'Could not load the employee.')),
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.busy.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    const id = this.employeeId();

    const request = id
      ? this.employees.update(id, value)
      : this.employees.create(value);

    request.subscribe({
      next: (employee) => {
        void this.router.navigate(['/employees', employee.id]);
      },
      error: (failure) => {
        this.busy.set(false);
        this.error.set(problemMessage(failure, 'Could not save the employee.'));
      },
    });
  }
}
