import { HttpErrorResponse } from '@angular/common/http';

interface ProblemDetail {
  title?: string;
  detail?: string;
}

/**
 * Pulls the human-readable part out of an RFC 9457 problem response.
 *
 * <p>The backend puts the reason a business rule was refused in `detail`, and
 * that sentence is what the HR Manager needs to see — not "422 Unprocessable
 * Content".
 */
export function problemMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Check your connection and try again.';
    }

    const problem = error.error as ProblemDetail | null;

    if (problem?.detail) {
      return problem.detail;
    }

    if (problem?.title) {
      return problem.title;
    }
  }

  return fallback;
}
