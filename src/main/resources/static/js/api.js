// API client for note operations
const Api = (() => {
    const jsonHeaders = (auditor) => ({
        'Content-Type': 'application/json',
        'X-User': auditor || 'system'
    });

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

    const fetchNotes = async ({ view, page, size, sort, query, auditor, tags, color, pinned }) => {
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
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const createNote = async (payload, auditor) => {
        const res = await fetch('/api/notes', {
            method: 'POST',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const updateNote = async (id, payload, auditor) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'PUT',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const patchNote = async (id, payload, auditor) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'PATCH',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const softDelete = async (id, auditor) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'DELETE',
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const restore = async (id, auditor) => {
        const res = await fetch(`/api/notes/${id}/restore`, {
            method: 'POST',
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const deletePermanent = async (id, auditor) => {
        const res = await fetch(`/api/notes/${id}/permanent`, {
            method: 'DELETE',
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const emptyTrash = async (auditor) => {
        const res = await fetch('/api/notes/deleted', {
            method: 'DELETE',
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const bulkAction = async (payload, auditor) => {
        const res = await fetch('/api/notes/bulk', {
            method: 'POST',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return parseResponse(res);
    };

    const fetchRevisions = async (id, auditor, page = 0, size = 5, sort) => {
        const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
        const res = await fetch(`/api/notes/${id}/revisions?page=${page}&size=${size}${sortParam}`, {
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const fetchRevision = async (id, revisionId, auditor) => {
        const res = await fetch(`/api/notes/${id}/revisions/${revisionId}`, {
            headers: jsonHeaders(auditor)
        });
        return parseResponse(res);
    };

    const restoreRevision = async (id, revisionId, auditor) => {
        const res = await fetch(`/api/notes/${id}/revisions/${revisionId}/restore`, {
            method: 'POST',
            headers: jsonHeaders(auditor)
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
