import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats a salary for display.
 *
 * <p>Fractions are dropped: annual salaries are read in thousands, and cents add
 * noise to every table and chart label. `null` renders as an em dash so "no
 * salary on record" is visibly different from a salary of zero.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, currency: string): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }).format(value);
  }
}

/** Compact form for chart axes and tick labels, e.g. 1.2M. */
@Pipe({ name: 'compactMoney' })
export class CompactMoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, currency: string): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }
}
