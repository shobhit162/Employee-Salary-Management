import { Pipe, PipeTransform } from '@angular/core';

import { formatCompactMoney, formatMoney } from './money.format';

/**
 * Formats a salary for display. `null` renders as an em dash, so "no salary on
 * record" is visibly different from a salary of zero.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, currency: string): string {
    return formatMoney(value, currency);
  }
}

/** Compact form, e.g. `$1.2M`. */
@Pipe({ name: 'compactMoney' })
export class CompactMoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, currency: string): string {
    return formatCompactMoney(value, currency);
  }
}
