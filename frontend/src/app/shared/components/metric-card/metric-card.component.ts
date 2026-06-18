import { AfterViewInit, Component, computed, input, signal } from '@angular/core';

@Component({
  selector: 'app-metric-card',
  templateUrl: './metric-card.component.html',
  styleUrl: './metric-card.component.scss'
})
export class MetricCardComponent implements AfterViewInit {
  readonly label      = input.required<string>();
  readonly value      = input.required<number>();
  readonly prefix     = input<string>('');
  readonly trend      = input<number | undefined>(undefined);
  readonly trendLabel = input<string>('');
  readonly decimals   = input<number>(2);

  private readonly _display = signal(0);

  readonly formattedValue = computed(() =>
    new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: this.decimals(),
      maximumFractionDigits: this.decimals(),
    }).format(this._display())
  );

  readonly hasTrend    = computed(() => this.trend() !== undefined);
  readonly trendUp     = computed(() => (this.trend() ?? 0) >= 0);
  readonly trendText   = computed(() =>
    this.trendLabel() || `${this.trendUp() ? '+' : ''}${this.trend()}%`
  );

  ngAfterViewInit(): void {
    const target   = this.value();
    const duration = 500;
    const start    = performance.now();

    const tick = (now: number) => {
      const t      = Math.min((now - start) / duration, 1);
      const eased  = 1 - Math.pow(1 - t, 3);
      this._display.set(target * eased);
      if (t < 1) requestAnimationFrame(tick);
      else this._display.set(target);
    };

    requestAnimationFrame(tick);
  }
}