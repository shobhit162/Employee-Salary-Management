import { Component, computed, input } from '@angular/core';

import { SalaryBand } from '../../../core/models/analytics.model';
import { CompactMoneyPipe } from '../../../shared/pipes/money/money.pipe';

/**
 * Salary distribution as a column chart.
 *
 * <p>The final band is open-ended, so its label reads "150k+" rather than
 * inventing an upper bound the data does not have.
 */
@Component({
  selector: 'app-histogram',
  imports: [CompactMoneyPipe],
  templateUrl: './histogram.component.html',
  styleUrl: './histogram.component.css',
})
export class HistogramComponent {
  readonly bands = input.required<SalaryBand[]>();
  readonly currency = input.required<string>();

  private readonly max = computed(() =>
    Math.max(1, ...this.bands().map((band) => band.employeeCount)),
  );

  protected heightOf(count: number): number {
    return (count / this.max()) * 100;
  }

  protected labelFor(band: SalaryBand): string {
    return band.upperBound === null
      ? `${band.lowerBound} and above`
      : `${band.lowerBound} to ${band.upperBound}`;
  }
}
