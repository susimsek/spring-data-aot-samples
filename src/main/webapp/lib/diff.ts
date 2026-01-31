const DIFF_ADD = 'add';
const DIFF_DEL = 'del';
const DIFF_EQ = 'eq';

export type DiffType = typeof DIFF_ADD | typeof DIFF_DEL | typeof DIFF_EQ;

export interface DiffOp {
  type: DiffType;
  value: string;
}

export function diffLinesDetailed(oldText: string, newText: string): DiffOp[] {
  const a = (oldText || '').split(/\r?\n/);
  const b = (newText || '').split(/\r?\n/);
  const m = a.length;
  const n = b.length;
  const lcs = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));
  for (let i = m - 1; i >= 0; i -= 1) {
    for (let j = n - 1; j >= 0; j -= 1) {
      if (a[i] === b[j]) {
        lcs[i][j] = 1 + lcs[i + 1][j + 1];
      } else {
        lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
      }
    }
  }
  const ops: DiffOp[] = [];
  let i = 0;
  let j = 0;
  while (i < m && j < n) {
    if (a[i] === b[j]) {
      ops.push({ type: DIFF_EQ, value: a[i] });
      i += 1;
      j += 1;
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
      ops.push({ type: DIFF_DEL, value: a[i] });
      i += 1;
    } else {
      ops.push({ type: DIFF_ADD, value: b[j] });
      j += 1;
    }
  }
  while (i < m) {
    ops.push({ type: DIFF_DEL, value: a[i] });
    i += 1;
  }
  while (j < n) {
    ops.push({ type: DIFF_ADD, value: b[j] });
    j += 1;
  }
  return ops;
}

export const diffTypes = {
  add: DIFF_ADD,
  del: DIFF_DEL,
  eq: DIFF_EQ,
};
