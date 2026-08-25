import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ChartConfiguration } from 'chart.js';

import { ChartComponent } from '../../../shared/chart/chart.component';
import { ChartTheme } from '../../../shared/chart/chart-theme';
import {
  formatCompactMoney,
  formatMoney,
} from '../../../shared/pipes/money/money.format';

export interface BarDatum {
  label: string;
  /** Total payroll for the group, in the reporting currency. */
  value: number;
  employeeCount?: number;
  average?: number;
}

/** Rows taller than this get their own scroll area rather than squashed bars. */
const ROW_HEIGHT = 34;
const MIN_HEIGHT = 200;

/**
 * Payroll by country or department, as horizontal bars.
 *
 * <p>Horizontal because the categories are words: country and department names
 * read straight across instead of being rotated under a vertical axis. The
 * per-bar detail that used to sit under every row now lives in the tooltip,
 * which is what removed the clutter — one line per group instead of three.
 */
@Component({
  selector: 'app-bar-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartComponent],
  template: `
    @if (data().length) {
      <app-chart
        type="bar"
        [data]="chartData()"
        [options]="chartOptions()"
        [height]="height()"
        [ariaLabel]="ariaLabel()"
      />
    } @else {
      <p class="muted">No data for these filters.</p>
    }
  `,
})
export class BarChartComponent {
  readonly data = input.required<BarDatum[]>();
  readonly currency = input.required<string>();

  private readonly theme = inject(ChartTheme);

  protected readonly height = computed(() =>
    Math.max(MIN_HEIGHT, this.data().length * ROW_HEIGHT),
  );

  protected readonly ariaLabel = computed(
    () =>
      `Payroll by group in ${this.currency()}. ` +
      this.data()
        .map((bar) => `${bar.label}: ${formatMoney(bar.value, this.currency())}`)
        .join('. '),
  );

  protected readonly chartData = computed<ChartConfiguration<'bar'>['data']>(
    () => {
      const palette = this.theme.palette();

      return {
        labels: this.data().map((bar) => bar.label),
        datasets: [
          {
            data: this.data().map((bar) => bar.value),
            backgroundColor: palette.accentSoft,
            hoverBackgroundColor: palette.accent,
            borderColor: palette.accent,
            borderWidth: 1,
            borderRadius: 4,
            barPercentage: 0.75,
          },
        ],
      };
    },
  );

  protected readonly chartOptions = computed<
    ChartConfiguration<'bar'>['options']
  >(() => {
    const palette = this.theme.palette();
    const rows = this.data();
    const currency = this.currency();

    return {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      layout: { padding: { right: 8 } },
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
            title: (items) => items[0]?.label ?? '',
            label: (item) => {
              const row = rows[item.dataIndex];
              const lines = [`Payroll  ${formatMoney(row.value, currency)}`];

              if (row.employeeCount !== undefined) {
                lines.push(`Employees  ${row.employeeCount.toLocaleString()}`);
              }
              if (row.average !== null && row.average !== undefined) {
                lines.push(`Average  ${formatMoney(row.average, currency)}`);
              }

              return lines;
            },
          },
        },
      },
      scales: {
        x: {
          border: { display: false },
          grid: { color: palette.grid },
          ticks: {
            color: palette.muted,
            callback: (value) => formatCompactMoney(Number(value), currency),
          },
        },
        y: {
          border: { display: false },
          grid: { display: false },
          ticks: { color: palette.text },
        },
      },
    };
  });


}
