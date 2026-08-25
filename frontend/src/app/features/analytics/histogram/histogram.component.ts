import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ChartConfiguration } from 'chart.js';

import { SalaryBand } from '../../../core/models/analytics.model';
import { ChartComponent } from '../../../shared/chart/chart.component';
import { ChartTheme } from '../../../shared/chart/chart-theme';
import {
  formatCompactMoney,
  formatMoney,
} from '../../../shared/pipes/money/money.format';

/**
 * Salary distribution as a column chart.
 *
 * <p>Only every other axis tick is labelled — with twenty-odd bands the labels
 * collided and the axis became unreadable. The exact range of any band is one
 * hover away, along with what share of the workforce sits in it, which is the
 * question a distribution is actually asked.
 */
@Component({
  selector: 'app-histogram',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartComponent],
  template: `
    @if (bands().length) {
      <app-chart
        type="bar"
        [data]="chartData()"
        [options]="chartOptions()"
        [height]="260"
        [ariaLabel]="ariaLabel()"
      />
    } @else {
      <p class="muted">No salaries to distribute for these filters.</p>
    }
  `,
})
export class HistogramComponent {
  readonly bands = input.required<SalaryBand[]>();
  readonly currency = input.required<string>();

  private readonly theme = inject(ChartTheme);

  private readonly total = computed(() =>
    this.bands().reduce((sum, band) => sum + band.employeeCount, 0),
  );

  protected readonly ariaLabel = computed(
    () =>
      `Salary distribution in ${this.currency()}. ` +
      this.bands()
        .map((band) => `${this.rangeOf(band)}: ${band.employeeCount} employees`)
        .join('. '),
  );

  protected readonly chartData = computed<ChartConfiguration<'bar'>['data']>(
    () => {
      const palette = this.theme.palette();

      return {
        labels: this.bands().map((band) => formatCompactMoney(band.lowerBound, this.currency())),
        datasets: [
          {
            data: this.bands().map((band) => band.employeeCount),
            backgroundColor: palette.accentSoft,
            hoverBackgroundColor: palette.accent,
            borderColor: palette.accent,
            borderWidth: 1,
            borderRadius: { topLeft: 3, topRight: 3, bottomLeft: 0, bottomRight: 0 },
            categoryPercentage: 0.9,
            barPercentage: 0.92,
          },
        ],
      };
    },
  );

  protected readonly chartOptions = computed<
    ChartConfiguration<'bar'>['options']
  >(() => {
    const palette = this.theme.palette();
    const bands = this.bands();
    const total = this.total();
    const currency = this.currency();

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: palette.surface,
          titleColor: palette.text,
          bodyColor: palette.muted,
          borderColor: palette.border,
          borderWidth: 1,
          padding: 10,
          displayColors: false,
          callbacks: {
            title: (items) => this.rangeOf(bands[items[0].dataIndex]),
            label: (item) => {
              const count = bands[item.dataIndex].employeeCount;
              const share = total ? Math.round((count / total) * 100) : 0;

              return `${count.toLocaleString()} employees · ${share}% of those shown`;
            },
          },
        },
      },
      scales: {
        x: {
          border: { display: false },
          grid: { display: false },
          ticks: {
            color: palette.muted,
            autoSkip: false,
            maxRotation: 0,
            // Every other label: adjacent band labels overlap otherwise.
            callback: (_value, index) =>
              index % 2 === 0
                ? formatCompactMoney(bands[index].lowerBound, currency)
                : '',
          },
        },
        y: {
          beginAtZero: true,
          border: { display: false },
          grid: { color: palette.grid },
          ticks: { color: palette.muted, precision: 0 },
          title: {
            display: true,
            text: 'Employees',
            color: palette.muted,
          },
        },
      },
    };
  });

  private rangeOf(band: SalaryBand): string {
    return band.upperBound === null
      ? `${formatCompactMoney(band.lowerBound, this.currency())} and above`
      : `${formatCompactMoney(band.lowerBound, this.currency())} – ` +
        `${formatCompactMoney(band.upperBound, this.currency())}`;
  }

}
