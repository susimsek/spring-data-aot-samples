import State from '/js/state.js';

// API client for note operations
const Api = (() => {
    const jsonHeaders = () => {
        const headers = {
            'Content-Type': 'application/json'
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

    const login = async (payload) => {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const currentUser = async () => {
        const res = await fetch('/api/auth/me', {
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const logout = async () => {
        const res = await fetch('/api/auth/logout', {
            method: 'POST',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const fetchNotes = async ({ view, page, size, sort, query, tags, color, pinned }) => {
        const base = view === 'trash' ? '/api/notes/deleted' : '/api/notes';
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
        const res = await fetch(`${base}?${params.toString()}`, {
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const createNote = async (payload) => {
        const res = await fetch('/api/notes', {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const updateNote = async (id, payload) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'PUT',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const patchNote = async (id, payload) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'PATCH',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const softDelete = async (id) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'DELETE',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const restore = async (id) => {
        const res = await fetch(`/api/notes/${id}/restore`, {
            method: 'POST',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const deletePermanent = async (id) => {
        const res = await fetch(`/api/notes/${id}/permanent`, {
            method: 'DELETE',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const emptyTrash = async () => {
        const res = await fetch('/api/notes/deleted', {
            method: 'DELETE',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const bulkAction = async (payload) => {
        const res = await fetch('/api/notes/bulk', {
            method: 'POST',
            headers: jsonHeaders(),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const fetchRevisions = async (id, page = 0, size = 5, sort) => {
        const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
        const res = await fetch(`/api/notes/${id}/revisions?page=${page}&size=${size}${sortParam}`, {
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const fetchRevision = async (id, revisionId) => {
        const res = await fetch(`/api/notes/${id}/revisions/${revisionId}`, {
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const restoreRevision = async (id, revisionId) => {
        const res = await fetch(`/api/notes/${id}/revisions/${revisionId}/restore`, {
            method: 'POST',
            headers: jsonHeaders()
        });
        return parseResponse(res);
    };

    const fetchTags = async (query) => {
        const url = new URL('/api/tags/suggest', window.location.origin);
        url.searchParams.set('page', 0);
        url.searchParams.set('size', 10);
        if (query) {
            url.searchParams.set('q', query);
        }
        const res = await fetch(url.toString(), {
            headers: jsonHeaders()
        });
        const body = await parseResponse(res);
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
