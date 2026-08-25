export interface Compensation {
  id: string;
  employeeId: string;
  amount: number;
  currency: string;
  effectiveFrom: string;
  effectiveTo: string | null;
}

/** Everything the salary tab shows, in one response. */
export interface CompensationSummary {
  employeeId: string;
  current: Compensation | null;
  scheduled: Compensation | null;
  history: Compensation[];
}

export interface CompensationWriteModel {
  amount: number;
  currency: string;
  effectiveFrom: string;
}
