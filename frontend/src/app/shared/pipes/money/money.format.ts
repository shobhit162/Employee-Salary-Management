/**
 * Salary formatting, in one place.
 *
 * <p>Templates reach these through the pipes below; Chart.js reaches them
 * directly, because its tick and tooltip callbacks are plain functions and
 * cannot use a pipe. Both paths must agree — an axis label reading `$1.2M`
 * beside a table cell reading `$1,150,000` is the kind of mismatch that makes a
 * dashboard look wrong even when the numbers are right.
 */
const NO_VALUE = '—';

export function formatMoney(
  value: number | null | undefined,
  currency: string,
): string {
  if (value === null || value === undefined) {
    return NO_VALUE;
  }

  // Fractions are dropped: annual salaries are read in thousands, and cents add
  // noise to every table and chart label.
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}

/** Abbreviated form for chart axes, e.g. `$1.2M`. */
export function formatCompactMoney(
  value: number | null | undefined,
  currency: string,
): string {
  if (value === null || value === undefined) {
    return NO_VALUE;
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(value);
}
