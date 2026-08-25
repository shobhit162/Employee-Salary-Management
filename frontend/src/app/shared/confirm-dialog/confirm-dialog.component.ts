import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  input,
  output,
  viewChild,
} from '@angular/core';

/**
 * Asks the user to confirm something consequential.
 *
 * <p>Built on the native `<dialog>` element rather than a hand-rolled overlay:
 * `showModal()` already provides focus trapping, Escape-to-dismiss, inertness of
 * the page behind it and top-layer stacking that no `z-index` can lose. That is
 * most of what makes a modal accessible, and none of it has to be written here.
 *
 * <p>It replaces `window.confirm`, which cannot be styled, blocks the whole
 * browser thread, and looks like a browser warning rather than part of the app.
 */
@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.css',
})
export class ConfirmDialogComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly message = input('');
  readonly confirmLabel = input('Confirm');
  readonly cancelLabel = input('Cancel');
  /** Styles the confirming action as destructive. */
  readonly danger = input(false);
  /** Disables the confirm button while the action is in flight. */
  readonly busy = input(false);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly dialog =
    viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const element = this.dialog().nativeElement;

      if (this.open() && !element.open) {
        element.showModal();
      } else if (!this.open() && element.open) {
        element.close();
      }
    });
  }

  protected confirm(): void {
    this.confirmed.emit();
  }

  protected cancel(): void {
    this.cancelled.emit();
  }

  /**
   * Escape closes the dialog natively; the parent still owns the open state, so
   * the dismissal has to be reported rather than silently diverging.
   */
  protected onCancelEvent(event: Event): void {
    event.preventDefault();
    this.cancelled.emit();
  }

  /** A click landing on the dialog itself is the backdrop, not the panel. */
  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.cancelled.emit();
    }
  }
}
