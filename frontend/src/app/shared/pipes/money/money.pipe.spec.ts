import { CompactMoneyPipe, MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats a salary with its currency symbol', () => {
    expect(pipe.transform(120000, 'USD')).toBe('$120,000');
  });

  it('drops cents, because salaries are read in thousands', () => {
    expect(pipe.transform(120000.49, 'USD')).toBe('$120,000');
  });

  it('uses the given currency, not a fixed one', () => {
    expect(pipe.transform(95000, 'GBP')).toBe('£95,000');
    expect(pipe.transform(2500000, 'INR')).toContain('2,500,000');
  });

  it('renders a dash when there is no salary on record', () => {
    // Distinct from a salary of zero, which is a real (if odd) value.
    expect(pipe.transform(null, 'USD')).toBe('—');
    expect(pipe.transform(undefined, 'USD')).toBe('—');
    expect(pipe.transform(0, 'USD')).toBe('$0');
  });
});

describe('CompactMoneyPipe', () => {
  const pipe = new CompactMoneyPipe();

  it('abbreviates large amounts for chart labels', () => {
    expect(pipe.transform(1_200_000, 'USD')).toBe('$1.2M');
    expect(pipe.transform(25_000, 'USD')).toBe('$25K');
  });

  it('renders a dash for a missing value', () => {
    expect(pipe.transform(null, 'USD')).toBe('—');
  });
});
