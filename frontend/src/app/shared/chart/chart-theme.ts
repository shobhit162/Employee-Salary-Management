import { Injectable, computed, signal } from '@angular/core';

export interface ChartPalette {
  accent: string;
  accentSoft: string;
  text: string;
  muted: string;
  grid: string;
  surface: string;
  border: string;
}

/**
 * Feeds the app's CSS custom properties into Chart.js.
 *
 * <p>Chart.js paints to a canvas, so it cannot inherit CSS the way the rest of
 * the UI does — colours have to be handed to it as strings. Reading them from
 * the same custom properties the stylesheet uses keeps one source of truth, and
 * watching `prefers-color-scheme` means the charts re-colour with the page
 * instead of staying light on a dark background.
 */
@Injectable({ providedIn: 'root' })
export class ChartTheme {
  private readonly revision = signal(0);

  readonly palette = computed<ChartPalette>(() => {
    this.revision();
    return this.read();
  });

  constructor() {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)');
    media?.addEventListener('change', () =>
      this.revision.update((value) => value + 1),
    );
  }

  private read(): ChartPalette {
    const styles = getComputedStyle(document.documentElement);
    const token = (name: string, fallback: string) =>
      styles.getPropertyValue(name).trim() || fallback;

    const accent = token('--accent', '#2f5fe0');

    return {
      accent,
      accentSoft: this.withAlpha(accent, 0.18),
      text: token('--text', '#12151a'),
      muted: token('--muted', '#667085'),
      grid: token('--border', '#e2e5ea'),
      surface: token('--surface', '#ffffff'),
      border: token('--border', '#e2e5ea'),
    };
  }

  /** Chart.js needs a concrete colour string; `color-mix` is not resolved on canvas. */
  private withAlpha(color: string, alpha: number): string {
    const hex = color.replace('#', '');

    if (!/^[0-9a-f]{6}$/i.test(hex)) {
      return color;
    }

    const r = parseInt(hex.slice(0, 2), 16);
    const g = parseInt(hex.slice(2, 4), 16);
    const b = parseInt(hex.slice(4, 6), 16);

    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
