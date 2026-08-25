import { HttpErrorResponse } from '@angular/common/http';

import { problemMessage } from './problem';

describe('problemMessage', () => {
  it('surfaces the reason a business rule refused the request', () => {
    // The `detail` field is the sentence the HR Manager needs to read; the
    // status code on its own tells them nothing actionable.
    const error = new HttpErrorResponse({
      status: 422,
      error: {
        title: 'Business Rule Violation',
        detail: 'A salary change must take effect on a future date',
      },
    });

    expect(problemMessage(error, 'fallback')).toBe(
      'A salary change must take effect on a future date',
    );
  });

  it('falls back to the problem title when there is no detail', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { title: 'Duplicate Resource' },
    });

    expect(problemMessage(error, 'fallback')).toBe('Duplicate Resource');
  });

  it('explains an unreachable server rather than showing status 0', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(problemMessage(error, 'fallback')).toContain('Cannot reach');
  });

  it('uses the caller fallback for a response with no problem body', () => {
    const error = new HttpErrorResponse({ status: 500, error: null });

    expect(problemMessage(error, 'Could not save.')).toBe('Could not save.');
  });

  it('uses the caller fallback for a non-HTTP failure', () => {
    expect(problemMessage(new TypeError('boom'), 'Could not save.')).toBe(
      'Could not save.',
    );
  });
});
