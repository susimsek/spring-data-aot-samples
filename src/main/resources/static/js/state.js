// Global state and auditor handling
const State = (() => {
    const USER_STORAGE_KEY = 'currentUser';

    function loadStoredUser() {
        try {
            const raw = localStorage.getItem(USER_STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function persistUser(user) {
        try {
            if (user) {
                localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
            } else {
                localStorage.removeItem(USER_STORAGE_KEY);
            }
        } catch (e) {
            // ignore storage errors
        }
    }

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
        currentUser: loadStoredUser()
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

    function saveToken() {
    }

    function clearToken() {
        state.currentUser = null;
        persistUser(null);
    }

    function currentUser() {
        return state.currentUser;
    }

    function isAdmin() {
        const authorities = state.currentUser?.authorities;
        return Array.isArray(authorities) && authorities.includes('ROLE_ADMIN');
    }

    function setCurrentUser(user) {
        state.currentUser = user || null;
        persistUser(state.currentUser);
    }

    return {
        state,
        currentAuditor,
        currentToken,
        saveToken,
        clearToken,
        currentUsername,
        setCurrentUser,
        isAdmin,
        currentUser
    };
})();

export default State;
