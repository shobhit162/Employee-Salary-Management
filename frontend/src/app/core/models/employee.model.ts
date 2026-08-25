import { Compensation } from './compensation.model';

export type EmploymentStatus = 'ACTIVE' | 'TERMINATED';

export interface Employee {
  id: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  department: string;
  jobTitle: string;
  employmentStatus: EmploymentStatus;
  terminationDate: string | null;
  createdAt: string;
  updatedAt: string;
}

/** A row of the employee list: the person plus what they earn today. */
export interface EmployeeListItem {
  employee: Employee;
  currentCompensation: Compensation | null;
}

export interface EmployeeFilterOptions {
  countryCodes: string[];
  departments: string[];
}

export interface EmployeeWriteModel {
  employeeCode?: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  department: string;
  jobTitle: string;
}

export interface EmployeeQuery {
  search?: string;
  countryCode?: string;
  department?: string;
  status?: EmploymentStatus;
  page: number;
  size: number;
  sortBy: string;
  direction: 'ASC' | 'DESC';
}

/** Mirrors Spring Data's serialised Page. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
