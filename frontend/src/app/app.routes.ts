import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

/**
 * Feature components are lazily loaded, so the login screen ships without the
 * dashboard's charts or the employee tables behind it.
 */
export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in · ACME Salary Management',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent,
      ),
  },
  {
    path: 'dashboard',
    title: 'Compensation dashboard · ACME',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/analytics/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent,
      ),
  },
  {
    path: 'employees',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        title: 'Employees · ACME',
        loadComponent: () =>
          import(
            './features/employees/employee-list/employee-list.component'
          ).then((m) => m.EmployeeListComponent),
      },
      {
        path: 'new',
        title: 'Add employee · ACME',
        loadComponent: () =>
          import(
            './features/employees/employee-form/employee-form.component'
          ).then((m) => m.EmployeeFormComponent),
      },
      {
        path: ':id',
        title: 'Employee · ACME',
        loadComponent: () =>
          import(
            './features/employees/employee-detail/employee-detail.component'
          ).then((m) => m.EmployeeDetailComponent),
      },
      {
        path: ':id/edit',
        title: 'Edit employee · ACME',
        loadComponent: () =>
          import(
            './features/employees/employee-form/employee-form.component'
          ).then((m) => m.EmployeeFormComponent),
      },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' },
];
