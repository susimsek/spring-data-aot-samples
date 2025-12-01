// API client for note operations
const Api = (() => {
    const jsonHeaders = (auditor) => ({
        'Content-Type': 'application/json',
        'X-Auditor': auditor || 'system'
    });

    const fetchNotes = async ({ view, page, size, sort, query, auditor }) => {
        const base = view === 'trash' ? '/api/notes/deleted' : '/api/notes';
        const qParam = query ? `&q=${encodeURIComponent(query)}` : '';
        const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
        const res = await fetch(`${base}?page=${page}&size=${size}${sortParam}${qParam}`, {
            headers: jsonHeaders(auditor)
        });
        if (!res.ok) throw new Error('Failed to load notes');
        return res.json();
    };

    const createNote = async (payload, auditor) => {
        const res = await fetch('/api/notes', {
            method: 'POST',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return res;
    };

    const updateNote = async (id, payload, auditor) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'PUT',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return res;
    };

    const patchNote = async (id, payload, auditor) => {
        const res = await fetch(`/api/notes/${id}`, {
            method: 'PATCH',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
        return res;
    };

    const softDelete = async (id, auditor) => {
        return fetch(`/api/notes/${id}`, {
            method: 'DELETE',
            headers: jsonHeaders(auditor)
        });
    };

    const restore = async (id, auditor) => {
        return fetch(`/api/notes/${id}/restore`, {
            method: 'POST',
            headers: jsonHeaders(auditor)
        });
    };

    const deletePermanent = async (id, auditor) => {
        return fetch(`/api/notes/${id}/permanent`, {
            method: 'DELETE',
            headers: jsonHeaders(auditor)
        });
    };

    const emptyTrash = async (auditor) => {
        return fetch('/api/notes/deleted', {
            method: 'DELETE',
            headers: jsonHeaders(auditor)
        });
    };

    const bulkAction = async (payload, auditor) => {
        return fetch('/api/notes/bulk', {
            method: 'POST',
            headers: jsonHeaders(auditor),
            body: JSON.stringify(payload)
        });
    };

    const fetchRevisions = async (id, auditor, page = 0, size = 5, sort) => {
        const sortParam = sort ? `&sort=${encodeURIComponent(sort)}` : '';
        const res = await fetch(`/api/notes/${id}/revisions?page=${page}&size=${size}${sortParam}`, {
            headers: jsonHeaders(auditor)
        });
        if (!res.ok) throw new Error('Failed to load revisions');
        return res.json();
    };

    const fetchRevision = async (id, revisionId, auditor) => {
        const res = await fetch(`/api/notes/${id}/revisions/${revisionId}`, {
            headers: jsonHeaders(auditor)
        });
        if (!res.ok) throw new Error('Revision not found');
        return res.json();
    };

    const restoreRevision = async (id, revisionId, auditor) => {
        return fetch(`/api/notes/${id}/revisions/${revisionId}/restore`, {
            method: 'POST',
            headers: jsonHeaders(auditor)
        });
    };

    return {
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
        restoreRevision
    };
})();

export default Api;
