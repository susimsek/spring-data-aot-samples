import { addDays, addHours, formatDate, toIsoString } from './format';

describe('format', () => {
  test('formatDate returns empty string for nullish values', () => {
    expect(formatDate(null)).toBe('');
    expect(formatDate(undefined)).toBe('');
    expect(formatDate('')).toBe('');
  });

  test('formatDate returns the input for invalid date values', () => {
    expect(formatDate('not-a-date')).toBe('not-a-date');
  });

  test('formatDate formats valid dates', () => {
    const value = new Date('2026-01-25T10:11:12.000Z');
    const formatted = formatDate(value);
    expect(formatted).toMatch(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}:\d{2}/);
  });

  test('toIsoString returns empty string for invalid date values', () => {
    expect(toIsoString('not-a-date')).toBe('');
  });

  test('toIsoString returns ISO string for a valid date', () => {
    expect(toIsoString(new Date('2026-01-25T10:11:12.000Z'))).toBe('2026-01-25T10:11:12.000Z');
    expect(toIsoString('2026-01-25T10:11:12.000Z')).toBe('2026-01-25T10:11:12.000Z');
  });

  test('addDays returns a new date and keeps original intact', () => {
    const base = new Date('2026-01-01T00:00:00.000Z');
    const next = addDays(base, 2);
    expect(next).not.toBe(base);
    expect(next.toISOString()).toBe('2026-01-03T00:00:00.000Z');
    expect(base.toISOString()).toBe('2026-01-01T00:00:00.000Z');
  });

  test('addHours returns a new date and keeps original intact', () => {
    const base = new Date('2026-01-01T00:00:00.000Z');
    const next = addHours(base, 5);
    expect(next).not.toBe(base);
    expect(next.toISOString()).toBe('2026-01-01T05:00:00.000Z');
    expect(base.toISOString()).toBe('2026-01-01T00:00:00.000Z');
  });
});
