import State from './state.js';

// API client for note operations
const Api = (() => {
    const jsonHeaders = () => {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
        return headers;
    };

    class ApiError extends Error {
        constructor(message, status, body, title) {
            super(message);
            this.status = status;
            this.body = body;
            this.title = title;
        }
    }

    let refreshInFlight = null;
    const redirectToLogin = () => {
        const path = window.location.pathname || '';
        if (path.includes('/login')) {
            return;
        }
        State.clearToken();
        const redirect = encodeURIComponent(`${window.location.pathname || '/'}${window.location.search || ''}${window.location.hash || ''}`);
        try {
            document.body.style.transition = 'opacity 120ms ease-in';
            document.body.style.opacity = '0';
            setTimeout(() => window.location.replace(`/login.html?redirect=${redirect}`), 130);
        } catch (e) {
            window.location.replace(`/login.html?redirect=${redirect}`);
        }
    };

    const parseResponse = async (res) => {
        let body = null;
        try {
            body = await res.json();
        } catch (e) {
            body = null;
        }
        if (!res.ok) {
            const title = body?.title;
            const message = body?.detail || title || res.statusText || 'Request failed';
            throw new ApiError(message, res.status, body, title);
        }
        return body ?? {};
    };

    const request = async (url, options = {}) => {
        try {
            const res = await fetch(url, options);
            return await parseResponse(res);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401) {
                try {
                    await refresh();
                    const res = await fetch(url, options);
                    return await parseResponse(res);
                } catch (refreshErr) {
                    redirectToLogin();
                    throw refreshErr;
                }
            }
            throw err;
        }
    };

    const refresh = async () => {
        if (!refreshInFlight) {
            refreshInFlight = fetch('/api/auth/refresh', {
                method: 'POST',
                headers: jsonHeaders()
            })
                .then(parseResponse)
                .catch((err) => {
                    redirectToLogin();
                    throw err instanceof ApiError ? err : new ApiError('Refresh failed', 401);
                })
                .finally(() => {
                    refreshInFlight = null;
                });
        }
        return refreshInFlight;
    };

    const login = async (payload) => {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const currentUser = async () => {
        return request('/api/auth/me', {headers: jsonHeaders()});
    };

    const noteBase = (suffix = '') => {
        const admin = State.isAdmin?.();
        const base = admin ? '/api/admin/notes' : '/api/notes';
        return `${base}${suffix}`;
    };

    const logout = async () => {
        const res = await fetch('/api/auth/logout', {
            method: 'POST',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const fetchNotes = async ({view, page, size, sort, query, tags, color, pinned}) => {
        const base = view === 'trash'
            ? noteBase('/deleted')
            : noteBase();
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', size);
        if (sort) params.set('sort', sort);
        if (query) params.set('q', query);
        if (color) params.set('color', color);
        if (typeof pinned === 'boolean') params.set('pinned', pinned);
        if (tags && Array.isArray(tags)) {
            tags.forEach(tag => params.append('tags', tag));
        }
        return request(`${base}?${params.toString()}`, {headers: jsonHeaders()});
    };

    const createNote = async (payload) => {
        return request(noteBase(), {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
    };

    const updateNote = async (id, payload) => {
        return request(`${noteBase()}/${id}`, {
            method: 'PUT',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
    };

    const patchNote = async (id, payload) => {
        return request(`${noteBase()}/${id}`, {
            method: 'PATCH',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
    };

    const softDelete = async (id) => {
        return request(`${noteBase()}/${id}`, {
            method: 'DELETE',
            headers: jsonHeaders()
        });
    };

    const restore = async (id) => {
        return request(`${noteBase()}/${id}/restore`, {
            method: 'POST',
            headers: jsonHeaders()
        });
    };

    const deletePermanent = async (id) => {
        return request(`${noteBase()}/${id}/permanent`, {
            method: 'DELETE',
            headers: jsonHeaders()
        });
    };

    const emptyTrash = async () => {
        return request(noteBase('/deleted'), {
            method: 'DELETE',
            headers: jsonHeaders()
        });
    };

    const bulkAction = async (payload) => {
        return request(`${noteBase()}/bulk`, {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
    };

    const fetchRevisions = async (id, page = 0, size = 5, sort) => {
        const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
        return request(`${noteBase()}/${id}/revisions?page=${page}&size=${size}${sortParam}`, {
            headers: jsonHeaders()
        });
    };

    const fetchRevision = async (id, revisionId) => {
        return request(`${noteBase()}/${id}/revisions/${revisionId}`, {
            headers: jsonHeaders()
        });
    };

    const restoreRevision = async (id, revisionId) => {
        return request(`${noteBase()}/${id}/revisions/${revisionId}/restore`, {
            method: 'POST',
            headers: jsonHeaders()
        });
    };

    const fetchTags = async (query) => {
        const url = new URL('/api/tags/suggest', window.location.origin);
        url.searchParams.set('page', 0);
        url.searchParams.set('size', 10);
        if (query) {
            url.searchParams.set('q', query);
        }
        const body = await request(url.toString(), {headers: jsonHeaders()});
        if (Array.isArray(body)) {
            return body;
        }
        if (body && Array.isArray(body.content)) {
            return body.content.map(t => t.name ?? t);
        }
        return [];
    };

    const searchUsers = async (query, page = 0, size = 10) => {
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', size);
        if (query) {
            params.set('q', query);
        }
        return request(`/api/admin/users/search?${params.toString()}`, {
            headers: jsonHeaders()
        });
    };

    const changeOwner = async (id, payload) => {
        return request(`/api/admin/notes/${id}/owner`, {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
    };

    const createShareLink = async (id, payload) => {
        return request(`${noteBase()}/${id}/share`, {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
    };

    const fetchShareLinks = async (id, page = 0, size = 3) => {
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', size);
        return request(`${noteBase()}/${id}/share?${params.toString()}`, {
            headers: jsonHeaders()
        });
    };

    const revokeShareLink = async (tokenId) => {
        const base = State.isAdmin?.() ? '/api/admin/notes' : '/api/notes';
        return request(`${base}/share/${tokenId}`, {
            method: 'DELETE',
            headers: jsonHeaders()
        });
    };

    const fetchNoteWithShareToken = async (id, token) => {
        if (!id || !token) {
            throw new ApiError('Share token is missing', 401);
        }
        const res = await fetch(`/api/notes/${id}?share_token=${encodeURIComponent(token)}`, {
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const fetchNoteWithShareTokenViaShareApi = async (token) => {
        if (!token) {
            throw new ApiError('Share token is missing', 401);
        }
        const res = await fetch(`/api/share/${encodeURIComponent(token)}`, {
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const fetchMyShareLinks = async (page = 0, size = 10, sort, query, status, createdFrom, createdTo) => {
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', size);
        if (sort) params.set('sort', sort);
        if (query) params.set('q', query);
        if (status && status !== 'all') params.set('status', status);
        if (createdFrom) params.set('createdFrom', createdFrom);
        if (createdTo) params.set('createdTo', createdTo);
        return request(`${noteBase('/share')}?${params.toString()}`, {
            headers: jsonHeaders()
        });
    };

    return {
        ApiError,
        login,
        currentUser,
        logout,
        refresh,
        headers: jsonHeaders,
        fetchNotes,
        createNote,
        updateNote,
        patchNote,
        softDelete,
        restore,
        deletePermanent,
        emptyTrash,
        bulkAction,
        fetchRevisions,
        fetchRevision,
        restoreRevision,
        fetchTags,
        searchUsers,
        changeOwner,
        createShareLink,
        fetchShareLinks,
        revokeShareLink,
        fetchNoteWithShareToken,
        fetchNoteWithShareTokenViaShareApi,
        fetchMyShareLinks
    };
})();

export default Api;
