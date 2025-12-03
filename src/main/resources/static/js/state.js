// Global state and auditor handling
const State = (() => {
    const TOKEN_KEY = 'notes.token';
    const USERNAME_KEY = 'notes.username';
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
        return 'system';
    }

    function currentToken() {
        return localStorage.getItem(TOKEN_KEY) || '';
    }

    function currentUsername() {
        return localStorage.getItem(USERNAME_KEY) || '';
    }

    function saveToken(token, username) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
        if (username) {
            localStorage.setItem(USERNAME_KEY, username);
        }
    }

    function clearToken() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
    }

    return { state, currentAuditor, currentToken, saveToken, clearToken, currentUsername };
})();

export default State;
