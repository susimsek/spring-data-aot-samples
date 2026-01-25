import { diffLinesDetailed, diffTypes } from './diff';

describe('diff', () => {
  test('diffLinesDetailed produces adds, deletes and equals', () => {
    const ops = diffLinesDetailed('a\nb\nc', 'a\nc\nd');
    expect(ops).toEqual([
      { type: diffTypes.eq, value: 'a' },
      { type: diffTypes.del, value: 'b' },
      { type: diffTypes.eq, value: 'c' },
      { type: diffTypes.add, value: 'd' },
    ]);
  });

  test('diffLinesDetailed handles empty input', () => {
    expect(diffLinesDetailed('', '')).toEqual([{ type: diffTypes.eq, value: '' }]);
    expect(diffLinesDetailed('', 'x')).toEqual([
      { type: diffTypes.del, value: '' },
      { type: diffTypes.add, value: 'x' },
    ]);
    expect(diffLinesDetailed('x', '')).toEqual([
      { type: diffTypes.del, value: 'x' },
      { type: diffTypes.add, value: '' },
    ]);
  });
});
