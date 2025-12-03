// Global state and auditor handling
const State = (() => {
    const AUDITOR_KEY = 'notes.auditor';
    const state = {
        page: 0,
        size: 10,
        total: 0,
        totalPages: 1,
        notes: [],
        mode: 'create',
        editId: null,
        deleteId: null,
        query: '',
        view: 'active',
        sort: 'createdDate,desc',
        selected: new Set(),
        filterTags: new Set(),
        filterColor: '',
        filterPinned: null
    };

    function currentAuditor() {
        const input = document.getElementById('auditorInput');
        const fromInput = input?.value?.trim();
        if (fromInput) return fromInput;
        const saved = localStorage.getItem(AUDITOR_KEY);
        return saved?.trim() || 'system';
    }

    function saveAuditor(value) {
        if (value && value.trim()) {
            localStorage.setItem(AUDITOR_KEY, value.trim());
        } else {
            localStorage.removeItem(AUDITOR_KEY);
        }
    }

    return { state, currentAuditor, saveAuditor };
})();

export default State;
