import { EmploymentStatus } from './employee.model';

export type BreakdownDimension = 'COUNTRY' | 'DEPARTMENT';

/** `null` for every average when nobody in the cohort has a salary on record. */
export interface SalaryStatistics {
  employeeCount: number;
  compensatedEmployeeCount: number;
  totalAnnualCompensation: number;
  average: number | null;
  median: number | null;
  minimum: number | null;
  maximum: number | null;
}

export interface SalarySummary {
  asOf: string;
  currency: string;
  statistics: SalaryStatistics;
}

export interface SalaryBreakdownRow {
  key: string;
  statistics: SalaryStatistics;
}

export interface SalaryBreakdown {
  asOf: string;
  currency: string;
  dimension: BreakdownDimension;
  rows: SalaryBreakdownRow[];
}

export interface SalaryBand {
  lowerBound: number;
  /** `null` on the final open-ended band. */
  upperBound: number | null;
  employeeCount: number;
}

export interface SalaryDistribution {
  asOf: string;
  currency: string;
  bandSize: number;
  bands: SalaryBand[];
}

/** The shared scope of every dashboard question. */
export interface AnalyticsFilters {
  countryCode: string | null;
  department: string | null;
  status: EmploymentStatus | 'ALL';
  currency: string;
}
