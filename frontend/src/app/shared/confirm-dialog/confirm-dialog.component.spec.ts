import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmDialogComponent } from './confirm-dialog.component';

describe('ConfirmDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let dialog: HTMLDialogElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
      providers: [provideZonelessChangeDetection()],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.componentRef.setInput('title', 'Terminate Ada Lovelace?');
    fixture.detectChanges();

    dialog = fixture.nativeElement.querySelector('dialog');
  });

  function buttonLabelled(text: string): HTMLButtonElement {
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    );
    const match = buttons.find((button) =>
      button.textContent?.trim().startsWith(text),
    );

    if (!match) {
      throw new Error(`No button labelled "${text}"`);
    }

    return match;
  }

  it('stays closed until asked to open', () => {
    expect(dialog.open).toBe(false);
  });

  it('opens modally, which is what traps focus and blocks the page behind', () => {
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    expect(dialog.open).toBe(true);
  });

  it('closes again when the parent withdraws it', () => {
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();
    fixture.componentRef.setInput('open', false);
    fixture.detectChanges();

    expect(dialog.open).toBe(false);
  });

  it('reports confirmation and dismissal separately', () => {
    const events: string[] = [];
    fixture.componentInstance.confirmed.subscribe(() => events.push('confirmed'));
    fixture.componentInstance.cancelled.subscribe(() => events.push('cancelled'));

    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('confirmLabel', 'Terminate');
    fixture.detectChanges();

    buttonLabelled('Terminate').click();
    buttonLabelled('Cancel').click();

    expect(events).toEqual(['confirmed', 'cancelled']);
  });

  it('reports an Escape dismissal, so the parent state cannot drift', () => {
    let cancelled = 0;
    fixture.componentInstance.cancelled.subscribe(() => (cancelled += 1));

    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    // What the browser fires when Escape is pressed on an open dialog.
    dialog.dispatchEvent(new Event('cancel', { cancelable: true }));

    expect(cancelled).toBe(1);
  });

  it('treats a click on the backdrop as a dismissal, but not one on the panel', () => {
    let cancelled = 0;
    fixture.componentInstance.cancelled.subscribe(() => (cancelled += 1));

    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.panel').click();
    expect(cancelled).toBe(0);

    dialog.click();
    expect(cancelled).toBe(1);
  });

  it('blocks a second confirmation while the first is still running', () => {
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('busy', true);
    fixture.componentRef.setInput('confirmLabel', 'Terminate');
    fixture.detectChanges();

    expect(buttonLabelled('Working').disabled).toBe(true);
  });
});
