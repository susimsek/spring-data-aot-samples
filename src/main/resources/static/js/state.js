// Global state and auditor handling
const State = (() => {
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
        return '';
    }

    function currentUsername() {
        return state.currentUser?.username || '';
    }

    function saveToken() {}

    function clearToken() {
        state.currentUser = null;
    }

    function setCurrentUser(user) {
        state.currentUser = user || null;
    }

    return { state, currentAuditor, currentToken, saveToken, clearToken, currentUsername, setCurrentUser };
})();

export default State;
