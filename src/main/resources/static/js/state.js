// Global state and auditor handling
const State = (() => {
    const TOKEN_KEY = 'notes.token';
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
        filterPinned: null,
        currentUser: null
    };

    function currentAuditor() {
        return 'system';
    }

    function currentToken() {
        return localStorage.getItem(TOKEN_KEY) || '';
    }

    function currentUsername() {
        return state.currentUser?.username || '';
    }

    function saveToken(token) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
    }

    function clearToken() {
        localStorage.removeItem(TOKEN_KEY);
        state.currentUser = null;
    }

    function setCurrentUser(user) {
        state.currentUser = user || null;
    }

    return { state, currentAuditor, currentToken, saveToken, clearToken, currentUsername, setCurrentUser };
})();

export default State;
