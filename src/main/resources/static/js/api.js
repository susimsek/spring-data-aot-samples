import State from '/js/state.js';

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
        window.location.replace('/login.html');
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

    const request = async (url, options = {}, { retry = true } = {}) => {
        try {
            const res = await fetch(url, options);
            return await parseResponse(res);
        } catch (err) {
            if (err instanceof ApiError && err.status === 401 && retry) {
                try {
                    await refresh();
                    const res = await fetch(url, options);
                    return await parseResponse(res);
                } catch (refreshErr) {
                    redirectToLogin();
                    throw refreshErr;
                }
            }
            if (err instanceof ApiError && err.status === 401) {
                redirectToLogin();
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
        return request('/api/auth/me', { headers: jsonHeaders() });
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

    const fetchNotes = async ({ view, page, size, sort, query, tags, color, pinned }) => {
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
        return request(`${base}?${params.toString()}`, { headers: jsonHeaders() });
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
        const body = await request(url.toString(), { headers: jsonHeaders() });
        if (Array.isArray(body)) {
            return body;
        }
        if (body && Array.isArray(body.content)) {
            return body.content.map(t => t.name ?? t);
        }
        return [];
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
        fetchTags
    };
})();

export default Api;
