import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild,
} from '@angular/core';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  ChartType,
  LinearScale,
  Tooltip,
} from 'chart.js';

// Registered explicitly rather than pulling in `registerables`: the dashboard
// draws bars only, and the unused controllers stay out of the bundle.
Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip);

/**
 * Thin wrapper around a Chart.js canvas.
 *
 * <p>Owns everything imperative — creating the chart, pushing new data into it,
 * reacting to a theme change, destroying it — so the chart components above stay
 * declarative and only describe *what* to plot.
 *
 * <p>Updates mutate the existing chart rather than recreating it, which keeps
 * Chart.js's transitions smooth when the dashboard filters change.
 */
@Component({
  selector: 'app-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="canvas-host" [style.height.px]="height()">
      <canvas #canvas [attr.aria-label]="ariaLabel()" role="img"></canvas>
    </div>
  `,
  styles: `
    .canvas-host {
      position: relative;
      width: 100%;
    }
  `,
})
export class ChartComponent implements OnDestroy {
  readonly type = input.required<ChartType>();
  readonly data = input.required<ChartConfiguration['data']>();
  readonly options = input<ChartConfiguration['options']>({});
  readonly height = input(280);
  readonly ariaLabel = input('');

  private readonly canvas =
    viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  private chart?: Chart;
  private renderedType?: ChartType;

  constructor() {
    effect(() => {
      const type = this.type();
      const data = this.data();
      const options = this.options();
      const element = this.canvas().nativeElement;

      // Chart.js cannot change type in place, so only a type change rebuilds.
      if (this.chart && this.renderedType !== type) {
        this.chart.destroy();
        this.chart = undefined;
      }

      if (this.chart) {
        this.chart.data = data;
        this.chart.options = options ?? {};
        this.chart.update();
        return;
      }

      this.chart = new Chart(element, { type, data, options });
      this.renderedType = type;
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
