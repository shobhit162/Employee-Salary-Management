import { Component, computed, input } from '@angular/core';

import { CompactMoneyPipe } from '../../../shared/pipes/money/money.pipe';

export interface BarDatum {
  label: string;
  value: number;
  /** Shown under the label, e.g. the headcount behind the bar. */
  caption?: string;
}

/**
 * A horizontal bar chart drawn with CSS.
 *
 * <p>Deliberately not a charting library: the dashboard needs two simple chart
 * shapes, and a library would add hundreds of kilobytes plus its own theming and
 * accessibility model for no gain. Bars are elements, so they reflow, scale with
 * the page font, follow the app's colour tokens in both themes, and expose a
 * meter role to screen readers without extra work.
 */
@Component({
  selector: 'app-bar-chart',
  imports: [CompactMoneyPipe],
  templateUrl: './bar-chart.component.html',
  styleUrl: './bar-chart.component.css',
})
export class BarChartComponent {
  readonly data = input.required<BarDatum[]>();
  readonly currency = input.required<string>();

  protected readonly max = computed(() =>
    Math.max(1, ...this.data().map((bar) => bar.value)),
  );

  protected widthOf(value: number): number {
    return (value / this.max()) * 100;
  }
}
