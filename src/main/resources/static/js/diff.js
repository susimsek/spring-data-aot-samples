// Simple line-based diff utility (no external deps)
const Diff = (() => {
    const DIFF_ADD = 'add';
    const DIFF_DEL = 'del';
    const DIFF_EQ = 'eq';

    function diffLinesDetailed(oldText, newText) {
        const a = (oldText || '').split(/\r?\n/);
        const b = (newText || '').split(/\r?\n/);
        const m = a.length;
        const n = b.length;
        const lcs = Array.from({length: m + 1}, () => new Array(n + 1).fill(0));
        for (let i = m - 1; i >= 0; i--) {
            for (let j = n - 1; j >= 0; j--) {
                if (a[i] === b[j]) {
                    lcs[i][j] = 1 + lcs[i + 1][j + 1];
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        const ops = [];
        let i = 0, j = 0;
        while (i < m && j < n) {
            if (a[i] === b[j]) {
                ops.push({type: DIFF_EQ, oldValue: a[i], newValue: b[j]});
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                ops.push({type: DIFF_DEL, oldValue: a[i], newValue: ''});
                i++;
            } else {
                ops.push({type: DIFF_ADD, oldValue: '', newValue: b[j]});
                j++;
            }
        }
        while (i < m) ops.push({type: DIFF_DEL, oldValue: a[i++], newValue: ''});
        while (j < n) ops.push({type: DIFF_ADD, oldValue: '', newValue: b[j++]});
        return ops;
    }

    function diffLines(oldText, newText) {
        const ops = diffLinesDetailed(oldText, newText);
        const html = ops.map(op => {
            const safeOld = escapeHtml(op.oldValue || op.newValue);
            if (op.type === DIFF_ADD) {
                return `<div class="text-success bg-success-subtle border border-success-subtle rounded px-2 py-1">+ ${safeOld}</div>`;
            }
            if (op.type === DIFF_DEL) {
                return `<div class="text-danger bg-danger-subtle border border-danger-subtle rounded px-2 py-1">− ${safeOld}</div>`;
            }
            return `<div class="text-body bg-body-secondary border rounded px-2 py-1">  ${safeOld}</div>`;
        }).join('');
        return html || '<div class="text-muted small">No differences.</div>';
    }

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    return {diffLines, diffLinesDetailed};
})();

export default Diff;
